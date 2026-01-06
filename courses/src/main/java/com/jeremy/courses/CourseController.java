package com.jeremy.courses;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    public CourseController(CourseRepository courseRepository,
                            UserRepository userRepository,
                            LessonRepository lessonRepository,
                            LessonProgressRepository lessonProgressRepository,
                            CourseEnrollmentRepository courseEnrollmentRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }

    private boolean isCourseAuthor(User user, Course course) {
        return user != null && course.getAuthor() != null && course.getAuthor().getId().equals(user.getId());
    }

    private boolean canSeeCourse(User user, Course course) {
        if (!course.isRestrictedToAllowList()) {
            // Unrestricted courses are visible to everyone (even anonymous)
            return true;
        }
        if (user == null) return false;
        if (isAdmin(user) || isCourseAuthor(user, course)) return true;
        return course.isEmailAllowed(user.getEmail());
    }

    // 1. GET method
    @GetMapping
    public ResponseEntity<?> getAllCourses(Authentication authentication) {
        User user = getCurrentUser(authentication);

        List<Course> all = courseRepository.findAll();
        List<Course> visible = new ArrayList<>();
        for (Course c : all) {
            if (canSeeCourse(user, c)) {
                visible.add(c);
            }
        }

        return ResponseEntity.ok(visible);
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

        List<Course> all = courseRepository.findAll();
        List<Course> created = new ArrayList<>();
        for (Course c : all) {
            if (c.getAuthor() != null && c.getAuthor().getId().equals(user.getId())) {
                created.add(c);
            }
        }
        return ResponseEntity.ok(created);
    }

    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    @GetMapping("/{courseId}/access")
    public ResponseEntity<?> getCourseAccess(@PathVariable Long courseId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Course not found"));
        }
        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not allowed to view access settings for this course"));
        }

        return ResponseEntity.ok(Map.of(
                "restrictedToAllowList", course.isRestrictedToAllowList(),
                "allowedEmails", course.getAllowedEmails()
        ));
    }

    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    @PutMapping("/{courseId}/access")
    public ResponseEntity<?> updateCourseAccess(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Course not found"));
        }
        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not allowed to modify access for this course"));
        }

        Object restrictedObj = body.get("restrictedToAllowList");
        boolean restricted = restrictedObj instanceof Boolean && (Boolean) restrictedObj;

        @SuppressWarnings("unchecked")
        List<String> emails = (List<String>) body.getOrDefault("allowedEmails", java.util.Collections.emptyList());
        Set<String> normalized = new HashSet<>();
        for (String e : emails) {
            if (e != null) {
                String trimmed = e.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }

        course.setRestrictedToAllowList(restricted);
        course.setAllowedEmails(normalized);
        courseRepository.save(course);

        return ResponseEntity.ok(Map.of(
                "restrictedToAllowList", course.isRestrictedToAllowList(),
                "allowedEmails", course.getAllowedEmails()
        ));
    }

    // 4. Delete a course (creator can delete their own courses, admin can delete any)
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Course not found"));
        }

        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not allowed to delete this course"));
        }

        // Clean up related data: lesson progress, lessons, enrollments, then course
        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        for (Lesson lesson : lessons) {
            lessonProgressRepository.deleteByLessonId(lesson.getId());
        }
        lessonRepository.deleteAll(lessons);

        // Remove enrollments for this course
        courseEnrollmentRepository.deleteAll(
                courseEnrollmentRepository.findAll().stream()
                        .filter(e -> e.getCourse().getId().equals(courseId))
                        .toList()
        );

        courseRepository.delete(course);

        return ResponseEntity.ok(Map.of("message", "Course deleted"));
    }
}
