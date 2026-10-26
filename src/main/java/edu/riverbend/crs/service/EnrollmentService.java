package edu.riverbend.crs.service;

import edu.riverbend.crs.domain.Course;
import edu.riverbend.crs.domain.Enrollment;
import edu.riverbend.crs.domain.EnrollmentStatus;
import edu.riverbend.crs.domain.Section;
import edu.riverbend.crs.domain.Student;
import edu.riverbend.crs.domain.Term;
import edu.riverbend.crs.exceptions.EnrollmentCheckedException;
import edu.riverbend.crs.exceptions.EnrollmentFailurePoint;
import edu.riverbend.crs.exceptions.EnrollmentRejectedException;
import edu.riverbend.crs.exceptions.ResourceNotFoundException;
import edu.riverbend.crs.exceptions.SeatAvailabilityPolicy;
import edu.riverbend.crs.repository.EnrollmentRepository;
import edu.riverbend.crs.repository.SectionRepository;
import edu.riverbend.crs.repository.StudentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private final StudentRepository students;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final SeatAvailabilityPolicy seatPolicy;
    private final EnrollmentFailurePoint failurePoint;
    private final Clock clock;

    public EnrollmentService(StudentRepository students, SectionRepository sections,
                             EnrollmentRepository enrollments, SeatAvailabilityPolicy seatPolicy,
                             EnrollmentFailurePoint failurePoint, Clock clock) {
        this.students = students;
        this.sections = sections;
        this.enrollments = enrollments;
        this.seatPolicy = seatPolicy;
        this.failurePoint = failurePoint;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long enroll(Long studentId, Long sectionId) throws EnrollmentCheckedException {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        Section section = sections.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found."));

        Term term = section.term;
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(term.registrationOpens) || now.isAfter(term.registrationCloses)) {
            throw new EnrollmentRejectedException("Registration window for " + term.name
                    + " is " + term.registrationOpens + " through " + term.registrationCloses + " UTC.");
        }

        if (enrollments.existsByStudentIdAndSectionId(studentId, sectionId)) {
            throw new EnrollmentRejectedException("Student is already enrolled in this section.");
        }

        for (Course prerequisite : section.course.prerequisites) {
            if (!enrollments.existsByStudentIdAndSectionCourseIdAndStatus(
                    studentId, prerequisite.id, EnrollmentStatus.COMPLETED)) {
                throw new EnrollmentRejectedException("Missing prerequisite: " + prerequisite.code);
            }
        }

        seatPolicy.check(section);

        failurePoint.beforeInsert();
        Enrollment enrollment = new Enrollment();
        enrollment.student = student;
        enrollment.section = section;
        enrollment.enrolledAt = now;
        enrollments.saveAndFlush(enrollment);

        failurePoint.beforeSeatIncrement();
        section.seatsTaken++;
        sections.saveAndFlush(section);
        return enrollment.id;
    }
}
