package edu.riverbend.crs.repository;
import edu.riverbend.crs.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentRepository extends JpaRepository<Student,Long> {  }
