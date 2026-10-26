package edu.riverbend.crs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "terms")
public class Term {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public LocalDateTime registrationOpens;
    @Column(nullable = false)
    public LocalDateTime registrationCloses;
}
