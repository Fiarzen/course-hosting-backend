package com.jeremy.courses;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // "Lob" stands for Large Object. It allows descriptions longer than 255
    // characters.
    @Lob
    private String description;

    // --- RELATIONSHIP ---

    // Many courses can belong to One User.
    // This creates a foreign key column "author_id" in the courses table.
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    // --- CONSTRUCTORS ---

    public Course() {
    }

    public Course(String title, String description, User author) {
        this.title = title;
        this.description = description;
        this.author = author;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}