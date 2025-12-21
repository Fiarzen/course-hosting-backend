package com.jeremy.courses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class LessonController {

    private final LessonRepository lessonRepository;

    public LessonController(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @GetMapping("/lessons")
    public List<Lesson> getAllLessons() {
        return lessonRepository.findAll();
    }

    // NEW ENDPOINT: Filter by Course ID
    // Usage: GET http://localhost:8080/lessons/course/1
    @GetMapping("/course/{courseId}")
    public List<Lesson> getLessonsByCourse(@PathVariable Long courseId) {
        return lessonRepository.findByCourseId(courseId);
    }
}
