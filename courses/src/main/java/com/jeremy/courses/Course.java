package com.jeremy.courses;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Description can be long; use TEXT instead of Large Object to avoid PostgreSQL LOB issues
    @Column(columnDefinition = "TEXT")
    private String description;

    // --- RELATIONSHIP ---

    // Many courses can belong to One User.
    // This creates a foreign key column "author_id" in the courses table.
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "restricted_to_allow_list", nullable = false)
    private boolean restrictedToAllowList = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_allowed_emails", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "email", nullable = false)
    private Set<String> allowedEmails = new HashSet<>();

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

    public boolean isRestrictedToAllowList() {
        return restrictedToAllowList;
    }

    public void setRestrictedToAllowList(boolean restrictedToAllowList) {
        this.restrictedToAllowList = restrictedToAllowList;
    }

    public Set<String> getAllowedEmails() {
        return allowedEmails;
    }

    public void setAllowedEmails(Set<String> allowedEmails) {
        this.allowedEmails = (allowedEmails != null) ? allowedEmails : new HashSet<>();
    }

    public boolean isEmailAllowed(String email) {
        if (!restrictedToAllowList) {
            // If not restricted, treat everyone as allowed via this helper
            return true;
        }
        if (email == null) return false;
        return allowedEmails != null && allowedEmails.contains(email.toLowerCase());
    }
}
