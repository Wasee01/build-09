package edu.riverbend.crs.exceptions;

import edu.riverbend.crs.domain.Section;

import org.springframework.stereotype.Component;

@Component
public class SeatAvailabilityPolicy {
    public void check(Section section) {
        if (section.seatsTaken >= section.capacity) {
            throw new EnrollmentRejectedException("No seats remain in this section.");
        }
    }
}
