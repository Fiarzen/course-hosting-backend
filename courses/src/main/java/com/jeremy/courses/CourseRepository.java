package com.jeremy.courses;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // We can add custom queries here later,
    // like findByAuthorId(Long authorId)
}
