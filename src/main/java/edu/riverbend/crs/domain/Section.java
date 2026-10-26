package edu.riverbend.crs.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sections")
public class Section {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    public Course course;
    @ManyToOne(optional = false)
    public Term term;
    @Column(nullable = false)
    public int capacity;
    @Column(name = "seats_taken", nullable = false)
    public int seatsTaken;
    @Version
    @Column(nullable = false)
    public Long version;
}
