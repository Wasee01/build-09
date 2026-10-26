package edu.riverbend.crs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.riverbend.crs.domain.*;
import edu.riverbend.crs.exceptions.EnrollmentCheckedException;
import edu.riverbend.crs.exceptions.EnrollmentFailurePoint;
import edu.riverbend.crs.exceptions.EnrollmentRejectedException;
import edu.riverbend.crs.exceptions.SeatAvailabilityPolicy;
import edu.riverbend.crs.repository.*;
import edu.riverbend.crs.service.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class EnrollmentIntegrationTest {
    @Autowired EnrollmentService service;
    @Autowired StudentRepository students;
    @Autowired CourseRepository courses;
    @Autowired TermRepository terms;
    @Autowired SectionRepository sections;
    @Autowired EnrollmentRepository enrollments;
    @Autowired TransactionTemplate tx;
    @Autowired TestSeatPolicy seatPolicy;
    @Autowired TestFailurePoint failurePoint;

    @TestConfiguration
    static class TestBeans {
        @Bean @Primary TestSeatPolicy testSeatPolicy() { return new TestSeatPolicy(); }
        @Bean @Primary TestFailurePoint testFailurePoint() { return new TestFailurePoint(); }
    }

    static class TestSeatPolicy extends SeatAvailabilityPolicy {
        volatile CountDownLatch reached;
        volatile CountDownLatch release;
        @Override public void check(Section section) {
            super.check(section);
            CountDownLatch a = reached, b = release;
            if (a != null && b != null) {
                a.countDown();
                try {
                    if (!b.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Race barrier timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted", e);
                }
            }
        }
    }

    static class TestFailurePoint implements EnrollmentFailurePoint {
        volatile String failAt;
        @Override public void beforeInsert() throws EnrollmentCheckedException {
            if ("step5".equals(failAt)) throw new EnrollmentCheckedException("Injected step 5 failure");
        }
        @Override public void beforeSeatIncrement() throws EnrollmentCheckedException {
            if ("step6".equals(failAt)) throw new EnrollmentCheckedException("Injected step 6 failure");
        }
    }

    record Fixture(Long sectionId, Long first, Long second) { }

    Fixture fixture(boolean open) {
        return tx.execute(status -> {
            Course course = new Course(); course.code = "CS-" + System.nanoTime();
            courses.save(course);
            Term term = new Term(); term.name = "Fall";
            LocalDateTime now = LocalDateTime.now();
            term.registrationOpens = open ? now.minusDays(1) : now.plusDays(1);
            term.registrationCloses = now.plusDays(3);
            terms.save(term);
            Section section = new Section(); section.course = course; section.term = term;
            section.capacity = 1; sections.save(section);
            Student first = new Student(); first.name = "First"; students.save(first);
            Student second = new Student(); second.name = "Second"; students.save(second);
            return new Fixture(section.id, first.id, second.id);
        });
    }

    void assertUnchanged(Fixture f) {
        tx.executeWithoutResult(status -> {
            assertThat(enrollments.countBySectionId(f.sectionId())).isZero();
            Section section = sections.findById(f.sectionId()).orElseThrow();
            assertThat(section.seatsTaken).isZero();
            assertThat(section.version).isZero();
        });
    }

    @Test
    void checkedFailureBeforeInsertRollsBack() {
        Fixture f = fixture(true);
        failurePoint.failAt = "step5";
        try {
            assertThatThrownBy(() -> service.enroll(f.first(), f.sectionId()))
                    .isInstanceOf(EnrollmentCheckedException.class);
        } finally { failurePoint.failAt = null; }
        assertUnchanged(f);
    }

    @Test
    void checkedFailureAfterInsertRollsBackInsertedRow() {
        Fixture f = fixture(true);
        failurePoint.failAt = "step6";
        try {
            assertThatThrownBy(() -> service.enroll(f.first(), f.sectionId()))
                    .isInstanceOf(EnrollmentCheckedException.class);
        } finally { failurePoint.failAt = null; }
        assertUnchanged(f);
    }

    @Test
    void outsideRegistrationWindowIsRejectedWithoutChanges() {
        Fixture f = fixture(false);
        assertThatThrownBy(() -> service.enroll(f.first(), f.sectionId()))
                .isInstanceOf(EnrollmentRejectedException.class)
                .hasMessageContaining("Registration window");
        assertUnchanged(f);
    }

    @Test
    void raceForOneSeatSucceedsExactlyOnceTenTimes() throws Exception {
        for (int run = 0; run < 10; run++) {
            Fixture f = fixture(true);
            CountDownLatch start = new CountDownLatch(1);
            seatPolicy.reached = new CountDownLatch(2);
            seatPolicy.release = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Boolean> a = pool.submit(() -> attempt(start, f.first(), f.sectionId()));
                Future<Boolean> b = pool.submit(() -> attempt(start, f.second(), f.sectionId()));
                start.countDown();
                assertThat(seatPolicy.reached.await(10, TimeUnit.SECONDS)).isTrue();
                seatPolicy.release.countDown();
                int successes = (a.get(20, TimeUnit.SECONDS) ? 1 : 0)
                        + (b.get(20, TimeUnit.SECONDS) ? 1 : 0);
                assertThat(successes).as("run %s", run).isEqualTo(1);
                tx.executeWithoutResult(status -> {
                    Section section = sections.findById(f.sectionId()).orElseThrow();
                    assertThat(section.seatsTaken).isEqualTo(section.capacity).isEqualTo(1);
                    assertThat(enrollments.countBySectionId(f.sectionId())).isEqualTo(1);
                    assertThat(section.version).isEqualTo(1L);
                });
            } finally {
                seatPolicy.reached = null;
                seatPolicy.release.countDown();
                seatPolicy.release = null;
                pool.shutdownNow();
            }
        }
    }

    private boolean attempt(CountDownLatch start, Long studentId, Long sectionId) throws Exception {
        start.await();
        try {
            service.enroll(studentId, sectionId);
            return true;
        } catch (DataAccessException | EnrollmentRejectedException ex) {
            return false;
        }
    }
}
