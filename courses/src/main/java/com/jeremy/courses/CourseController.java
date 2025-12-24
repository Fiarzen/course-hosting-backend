package com.jeremy.courses;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
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
}