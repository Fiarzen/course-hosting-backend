package com.jeremy.courses;

import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob // Allows long text for lesson notes
    private String content;

    private String videoUrl; // Link to the video file (e.g., YouTube or S3)

    private String pdfUrl; // Link to the PDF resource

    // --- RELATIONSHIP ---

    // Many lessons belong to One Course.
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    // --- CONSTRUCTORS ---

    public Lesson() {
    }

    public Lesson(String title, String content, String videoUrl, String pdfUrl, Course course) {
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.pdfUrl = pdfUrl;
        this.course = course;
    }

    // --- GETTERS & SETTERS ---

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}