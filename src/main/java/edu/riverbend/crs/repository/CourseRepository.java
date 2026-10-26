package edu.riverbend.crs.repository;
import edu.riverbend.crs.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseRepository extends JpaRepository<Course,Long> {  }
