package edu.riverbend.crs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentApiTest {
    @Autowired MockMvc mvc;

    @Test
    void missingStudentReturns404ProblemDetail() throws Exception {
        mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":999999,\"sectionId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogEndpointsAreAvailable() throws Exception {
        mvc.perform(get("/api/courses")).andExpect(status().isOk());
        mvc.perform(get("/api/terms")).andExpect(status().isOk());
        mvc.perform(get("/api/students")).andExpect(status().isOk());
        mvc.perform(get("/api/sections")).andExpect(status().isOk());
    }
}
