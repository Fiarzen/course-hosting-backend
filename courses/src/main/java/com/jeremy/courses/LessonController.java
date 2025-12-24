package com.jeremy.courses;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lessons")
public class LessonController {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public LessonController(LessonRepository lessonRepository, CourseRepository courseRepository,
                           UserRepository userRepository, S3Service s3Service) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    @GetMapping
    public List<Lesson> getAllLessons() {
        return lessonRepository.findAll();
    }

    // Usage: GET http://localhost:8080/lessons/course/1
    @GetMapping("/course/{courseId}")
    public List<Lesson> getLessonsByCourse(@PathVariable Long courseId) {
        return lessonRepository.findByCourseId(courseId);
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

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));

        // Check if user is admin or course author
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean isCourseAuthor = course.getAuthor() != null && 
                                course.getAuthor().getId().equals(user.getId());

        if (!isAdmin && !isCourseAuthor) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only course authors or admins can create lessons for this course"));
        }

        // Upload PDF to S3 (or local storage if S3 not configured)
        String pdfUrl = null;
        if (pdfFile != null && !pdfFile.isEmpty()) {
            pdfUrl = s3Service.uploadPdf(pdfFile);
        }

        Lesson lesson = new Lesson(title, content, videoUrl, pdfUrl, course);
        Lesson savedLesson = lessonRepository.save(lesson);
        return ResponseEntity.ok(savedLesson);
    }
}
