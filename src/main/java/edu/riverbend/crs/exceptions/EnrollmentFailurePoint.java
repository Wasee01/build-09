package edu.riverbend.crs.exceptions;

public interface EnrollmentFailurePoint {
    default void beforeInsert() throws EnrollmentCheckedException {
    }

    default void beforeSeatIncrement() throws EnrollmentCheckedException {
    }
}
