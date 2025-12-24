package com.jeremy.courses;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseController(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    // 1. GET method
    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // 2. POST method (Make sure this is OUTSIDE the method above!)
    @PostMapping
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    public Course createCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }

    // 3. Get courses created by the currently authenticated creator/admin
    @GetMapping("/my-created")
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getMyCreatedCourses(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        java.util.List<Course> all = courseRepository.findAll();
        java.util.List<Course> created = new java.util.ArrayList<>();
        for (Course c : all) {
            if (c.getAuthor() != null && c.getAuthor().getId().equals(user.getId())) {
                created.add(c);
            }
        }
        return ResponseEntity.ok(created);
    }
}
