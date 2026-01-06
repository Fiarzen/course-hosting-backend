package com.jeremy.courses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public EnrollmentController(
            CourseEnrollmentRepository enrollmentRepository,
            LessonProgressRepository progressRepository,
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    // Enroll in a course
    @PostMapping("/courses/{courseId}")
    public ResponseEntity<?> enrollInCourse(@PathVariable Long courseId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Course not found"));
        }

        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean isAuthor = course.getAuthor() != null && course.getAuthor().getId().equals(user.getId());
        if (course.isRestrictedToAllowList() && !isAdmin && !isAuthor && !course.isEmailAllowed(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Enrollment restricted: you are not on this course's allowlist"));
        }

        // Check if already enrolled
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Already enrolled in this course"));
        }

        CourseEnrollment enrollment = new CourseEnrollment(user, course);
        enrollmentRepository.save(enrollment);

        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    // Get user's enrolled courses
    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        List<CourseEnrollment> enrollments = enrollmentRepository.findByUserId(user.getId());
        List<Map<String, Object>> coursesWithProgress = enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();
            List<Lesson> lessons = lessonRepository.findByCourseId(course.getId());
            long totalLessons = lessons.size();
            long completedLessons = progressRepository.countByUserIdAndLessonCourseIdAndCompleted(
                    user.getId(), course.getId(), true);

            return Map.of(
                    "course", course,
                    "enrolledAt", enrollment.getEnrolledAt().toString(),
                    "totalLessons", totalLessons,
                    "completedLessons", completedLessons,
                    "progress", totalLessons > 0 ? (completedLessons * 100.0 / totalLessons) : 0.0
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(coursesWithProgress);
    }

    // Unenroll from a course
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<?> unenrollFromCourse(@PathVariable Long courseId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Course not found"));
        }

        var enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId);
        if (enrollmentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "You are not enrolled in this course"));
        }

        // Delete lesson progress for this user+course
        List<LessonProgress> progressEntries = progressRepository.findByUserIdAndLessonCourseId(user.getId(), courseId);
        progressRepository.deleteAll(progressEntries);

        // Delete enrollment
        enrollmentRepository.delete(enrollmentOpt.get());

        return ResponseEntity.ok(Map.of("message", "Unenrolled from course"));
    }

    // Mark lesson as completed
    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<?> completeLesson(@PathVariable Long lessonId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Lesson not found"));
        }

        // Check if user is enrolled in the course
        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), lesson.getCourse().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You must be enrolled in the course to complete lessons"));
        }

        LessonProgress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(new LessonProgress(user, lesson));
        progress.setCompleted(true);
        progressRepository.save(progress);

        return ResponseEntity.ok(progress);
    }

    // Get progress for a specific course
    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<?> getCourseProgress(@PathVariable Long courseId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        // Check if enrolled
        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not enrolled in this course"));
        }

        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        List<Map<String, Object>> lessonProgress = lessons.stream().map(lesson -> {
            LessonProgress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lesson.getId())
                    .orElse(new LessonProgress(user, lesson));

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("lesson", lesson);
            map.put("completed", progress.isCompleted());
            // Allow completedAt to be null in the JSON payload without causing Map.of NPE
            map.put("completedAt", progress.getCompletedAt() != null ? progress.getCompletedAt().toString() : null);
            return map;
        }).collect(Collectors.toList());

        long completedCount = lessonProgress.stream().filter(p -> (Boolean) p.get("completed")).count();
        double progressPercent = lessons.size() > 0 ? (completedCount * 100.0 / lessons.size()) : 0.0;

        return ResponseEntity.ok(Map.of(
                "lessons", lessonProgress,
                "totalLessons", lessons.size(),
                "completedLessons", completedCount,
                "progress", progressPercent
        ));
    }
}


