package edu.riverbend.crs.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String name;
}
