package com.jeremy.courses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class CourseController {

    private final CourseRepository courseRepository;

    // 2. Update Constructor to include LessonRepository
    public CourseController(CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
    }

    // Map requests to /courses
    @GetMapping("/courses")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

}
