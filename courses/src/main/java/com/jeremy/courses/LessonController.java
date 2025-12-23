package com.jeremy.courses;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lessons")
public class LessonController {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    public LessonController(LessonRepository lessonRepository, CourseRepository courseRepository) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
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

    // Create a lesson with optional video and/or PDF uploads.
    // Expects multipart/form-data with fields:
    // - title (text)
    // - content (text)
    // - courseId (number)
    // - video (file, optional)
    // - pdf (file, optional)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CREATOR')")
    public Lesson createLesson(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("courseId") Long courseId,
            @RequestPart(value = "video", required = false) MultipartFile videoFile,
            @RequestPart(value = "pdf", required = false) MultipartFile pdfFile
    ) throws IOException {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));

        String uploadRoot = "uploads"; // relative to application working directory

        Path videoDir = Paths.get(uploadRoot, "videos");
        Path pdfDir = Paths.get(uploadRoot, "pdfs");

        Files.createDirectories(videoDir);
        Files.createDirectories(pdfDir);

        String videoUrl = null;
        if (videoFile != null && !videoFile.isEmpty()) {
            String videoFilename = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
            Path videoPath = videoDir.resolve(videoFilename);
            Files.copy(videoFile.getInputStream(), videoPath, StandardCopyOption.REPLACE_EXISTING);
            videoUrl = "/files/videos/" + videoFilename;
        }

        String pdfUrl = null;
        if (pdfFile != null && !pdfFile.isEmpty()) {
            String pdfFilename = UUID.randomUUID() + "_" + pdfFile.getOriginalFilename();
            Path pdfPath = pdfDir.resolve(pdfFilename);
            Files.copy(pdfFile.getInputStream(), pdfPath, StandardCopyOption.REPLACE_EXISTING);
            pdfUrl = "/files/pdfs/" + pdfFilename;
        }

        Lesson lesson = new Lesson(title, content, videoUrl, pdfUrl, course);
        return lessonRepository.save(lesson);
    }
}
