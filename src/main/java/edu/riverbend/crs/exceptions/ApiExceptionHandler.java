package edu.riverbend.crs.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ProblemDetail> conflict(ConcurrencyFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Seat no longer available",
                "The last seat was taken by another enrollment. Refresh and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> constraint(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Enrollment conflict",
                "Enrollment violates a database constraint or the section is full.");
    }

    @ExceptionHandler(EnrollmentRejectedException.class)
    public ResponseEntity<ProblemDetail> rejected(EnrollmentRejectedException ex) {
        return problem(HttpStatus.CONFLICT, "Enrollment rejected", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> missing(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> badRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Check the request fields and values.");
    }

    @ExceptionHandler(EnrollmentCheckedException.class)
    public ResponseEntity<ProblemDetail> checked(EnrollmentCheckedException ex) {
        return problem(HttpStatus.CONFLICT, "Enrollment failed", "Enrollment was rolled back.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred.");
    }
}
