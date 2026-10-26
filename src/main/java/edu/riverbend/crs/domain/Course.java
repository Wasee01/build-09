package edu.riverbend.crs.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String code;
    @ManyToMany
    @JoinTable(name = "course_prerequisites", joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisites_id"))
    public Set<Course> prerequisites = new HashSet<>();
}
