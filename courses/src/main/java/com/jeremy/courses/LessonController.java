package com.jeremy.courses;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/lessons")
public class LessonController {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final S3Service s3Service;

    public LessonController(
            LessonRepository lessonRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseEnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository,
            S3Service s3Service
    ) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.s3Service = s3Service;
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

    private boolean isEnrolledInCourse(User user, Long courseId) {
        if (user == null || courseId == null) {
            return false;
        }
        return enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
    }

    private boolean canViewFullLessonContent(User user, Course course) {
        if (user == null || course == null) {
            return false;
        }
        return isAdmin(user) || isCourseAuthor(user, course) || isEnrolledInCourse(user, course.getId());
    }

    @GetMapping
    public ResponseEntity<?> getAllLessons(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        // Admins can see all lessons; others only lessons for courses they authored or are enrolled in
        if (isAdmin(user)) {
            return ResponseEntity.ok(lessonRepository.findAll());
        }

        List<CourseEnrollment> enrollments = enrollmentRepository.findByUserId(user.getId());
        Set<Long> accessibleCourseIds = new HashSet<>();
        for (CourseEnrollment e : enrollments) {
            accessibleCourseIds.add(e.getCourse().getId());
        }
        for (Course c : courseRepository.findAll()) {
            if (c.getAuthor() != null && c.getAuthor().getId().equals(user.getId())) {
                accessibleCourseIds.add(c.getId());
            }
        }

        List<Lesson> allLessons = lessonRepository.findAll();
        List<Lesson> filtered = new ArrayList<>();
        for (Lesson lesson : allLessons) {
            if (lesson.getCourse() != null && accessibleCourseIds.contains(lesson.getCourse().getId())) {
                filtered.add(lesson);
            }
        }

        return ResponseEntity.ok(filtered);
    }

    // Usage: GET http://localhost:8080/lessons/course/1
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getLessonsByCourse(@PathVariable Long courseId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found"));
        }

        List<Lesson> orderedLessons = lessonRepository.findByCourseIdOrderByOrderIndexAscIdAsc(courseId);

        // If user cannot view full content, return only basic information (id, title, orderIndex)
        if (!canViewFullLessonContent(user, course)) {
            List<Map<String, Object>> summaries = new ArrayList<>();
            int index = 0;
            for (Lesson lesson : orderedLessons) {
                summaries.add(Map.of(
                        "id", lesson.getId(),
                        "title", lesson.getTitle(),
                        "orderIndex", lesson.getOrderIndex(),
                        "position", ++index
                ));
            }
            return ResponseEntity.ok(summaries);
        }

        return ResponseEntity.ok(orderedLessons);
    }

    // Create a lesson with optional YouTube URL and/or PDF upload.
    // Expects multipart/form-data with fields:
    // - title (text)
    // - content (text)
    // - courseId (number)
    // - videoUrl (text, optional) - YouTube URL
    // - pdf (file, optional)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createLesson(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestPart(value = "pdf", required = false) MultipartFile pdfFile,
            Authentication authentication
    ) throws IOException {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));

        // Check if user is admin or course author
        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only course authors or admins can create lessons for this course"));
        }

        // Upload PDF to S3 (or local storage if S3 not configured)
        String pdfUrl = null;
        if (pdfFile != null && !pdfFile.isEmpty()) {
            pdfUrl = s3Service.uploadPdf(pdfFile);
        }

        Lesson lesson = new Lesson(title, content, videoUrl, pdfUrl, course);

        // Set orderIndex to the next position in this course
        Integer nextIndex = lessonRepository.findByCourseId(courseId).size() + 1;
        lesson.setOrderIndex(nextIndex);

        Lesson savedLesson = lessonRepository.save(lesson);
        return ResponseEntity.ok(savedLesson);
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<?> getLessonById(@PathVariable Long lessonId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Lesson not found"));
        }

        Course course = lesson.getCourse();
        if (!canViewFullLessonContent(user, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You must be enrolled in the course (or be the author/admin) to view this lesson"));
        }

        return ResponseEntity.ok(lesson);
    }

    @PutMapping(value = "/{lessonId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateLesson(
            @PathVariable Long lessonId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam(value = "clearPdf", required = false, defaultValue = "false") boolean clearPdf,
            @RequestPart(value = "pdf", required = false) MultipartFile pdfFile,
            Authentication authentication
    ) throws IOException {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Lesson not found"));
        }

        Course course = lesson.getCourse();
        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only course authors or admins can update lessons for this course"));
        }

        // Update basic fields
        lesson.setTitle(title);
        lesson.setContent(content);
        if (videoUrl != null) {
            lesson.setVideoUrl(videoUrl);
        }

        // Handle PDF updates
        if (pdfFile != null && !pdfFile.isEmpty()) {
            String pdfUrl = s3Service.uploadPdf(pdfFile);
            lesson.setPdfUrl(pdfUrl);
        } else if (clearPdf) {
            lesson.setPdfUrl(null);
        }

        Lesson saved = lessonRepository.save(lesson);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteLesson(@PathVariable Long lessonId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Lesson not found"));
        }

        Course course = lesson.getCourse();
        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only course authors or admins can delete lessons for this course"));
        }

        // Clean up lesson progress records
        lessonProgressRepository.deleteByLessonId(lessonId);

        lessonRepository.delete(lesson);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/course/{courseId}/reorder")
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> reorderLessons(
            @PathVariable Long courseId,
            @RequestBody List<Long> orderedLessonIds,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found"));
        }

        if (!isAdmin(user) && !isCourseAuthor(user, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only course authors or admins can reorder lessons for this course"));
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAscIdAsc(courseId);
        Map<Long, Lesson> byId = new java.util.HashMap<>();
        for (Lesson lesson : lessons) {
            byId.put(lesson.getId(), lesson);
        }

        int index = 1;
        for (Long id : orderedLessonIds) {
            Lesson lesson = byId.get(id);
            if (lesson != null) {
                lesson.setOrderIndex(index++);
            }
        }

        // Any lessons not in the submitted list will be appended after
        for (Lesson lesson : lessons) {
            if (lesson.getOrderIndex() == null || lesson.getOrderIndex() < 1) {
                lesson.setOrderIndex(index++);
            }
        }

        lessonRepository.saveAll(lessons);

        return ResponseEntity.ok(lessonRepository.findByCourseIdOrderByOrderIndexAscIdAsc(courseId));
    }
}
