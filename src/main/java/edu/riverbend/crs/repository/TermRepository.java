package edu.riverbend.crs.repository;
import edu.riverbend.crs.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TermRepository extends JpaRepository<Term,Long> {  }
