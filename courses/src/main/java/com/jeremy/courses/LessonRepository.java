package com.jeremy.courses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    // Spring Data JPA magic: It looks for a "course" field in Lesson,
    // and an "id" field in Course.
    List<Lesson> findByCourseId(Long courseId);
}