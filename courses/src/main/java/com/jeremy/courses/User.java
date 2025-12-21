package com.jeremy.courses;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. We added 'name' back so your Getters work
    private String name;

    private String email;
    private String password;
    private String role;

    // --- CONSTRUCTORS ---

    // Required by JPA
    public User() {
    }

    // Constructor 1: Keep this so your DataSeeder doesn't break!
    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.password = "defaultPassword"; // Temporary default
        this.role = "STUDENT"; // Temporary default
    }

    // Constructor 2: The new one you added for Auth/Login
    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // --- GETTERS & SETTERS ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Added these so you can actually access the new fields
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}