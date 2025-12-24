package com.jeremy.courses;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class EnrollmentAllowlistIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void nonAllowlistedStudentCannotEnrollInRestrictedCourse() throws Exception {
        // Use seeded admin as author and seeded student as the user trying to enroll.
        User admin = userRepository.findByEmail("admin@example.com");
        User student = userRepository.findByEmail("student@example.com");
        assertNotNull(admin, "Expected seeded admin@example.com user to exist");
        assertNotNull(student, "Expected seeded student@example.com user to exist");

        Course restricted = new Course("Restricted Course", "Only for certain emails", admin);
        restricted.setRestrictedToAllowList(true);
        // Do NOT include student@example.com here
        restricted.setAllowedEmails(Set.of("other@example.com"));
        restricted = courseRepository.save(restricted);

        mockMvc.perform(post("/enrollments/courses/{courseId}", restricted.getId())
                        .with(httpBasic("student@example.com", "pass321")))
                .andExpect(status().isUnauthorized());
    }
}
