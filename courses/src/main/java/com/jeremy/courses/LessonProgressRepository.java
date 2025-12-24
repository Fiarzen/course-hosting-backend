package com.jeremy.courses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    List<LessonProgress> findByUserId(Long userId);
    List<LessonProgress> findByUserIdAndLessonCourseId(Long userId, Long courseId);
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    long countByUserIdAndLessonCourseIdAndCompleted(Long userId, Long courseId, boolean completed);

    void deleteByLessonId(Long lessonId);
}


