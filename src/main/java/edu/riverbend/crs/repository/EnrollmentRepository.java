package edu.riverbend.crs.repository;

import edu.riverbend.crs.domain.Enrollment;
import edu.riverbend.crs.domain.EnrollmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);
    boolean existsByStudentIdAndSectionCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);
    List<Enrollment> findByStudentId(Long studentId);
    long countBySectionId(Long sectionId);
}
