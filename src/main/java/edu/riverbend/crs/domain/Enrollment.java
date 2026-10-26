package edu.riverbend.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"}))
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    public Student student;
    @ManyToOne(optional = false)
    public Section section;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EnrollmentStatus status = EnrollmentStatus.ENROLLED;
    @Column(nullable = false)
    public LocalDateTime enrolledAt;
}
