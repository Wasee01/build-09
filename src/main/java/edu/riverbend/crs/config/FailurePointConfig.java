package edu.riverbend.crs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.riverbend.crs.exceptions.EnrollmentFailurePoint;

@Configuration
public class FailurePointConfig {
    @Bean
    EnrollmentFailurePoint enrollmentFailurePoint() {
        return new EnrollmentFailurePoint() { };
    }
}
