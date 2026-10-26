package edu.riverbend.crs.web;

import edu.riverbend.crs.exceptions.EnrollmentCheckedException;
import edu.riverbend.crs.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) { this.service = service; }

    public record EnrollmentRequest(@NotNull @Positive Long studentId, @NotNull @Positive Long sectionId) { }
    public record EnrollmentResponse(Long id) { }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentRequest request)
            throws EnrollmentCheckedException {
        Long id = service.enroll(request.studentId(), request.sectionId());
        return ResponseEntity.created(URI.create("/api/enrollments/" + id)).body(new EnrollmentResponse(id));
    }
}
