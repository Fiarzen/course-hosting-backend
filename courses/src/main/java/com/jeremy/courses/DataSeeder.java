package com.jeremy.courses;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    // 1. Inject BOTH repositories
    public DataSeeder(UserRepository userRepository, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only run if the database is empty of users
        if (userRepository.count() == 0) {
            System.out.println("🌱 Seeding database...");

            // --- Create Users ---
            User admin = new User("admin@example.com", "password123", "CREATOR");
            admin.setName("Admin User");

            User student = new User("student@example.com", "pass321", "STUDENT");
            student.setName("Jeremy Student");

            // Save users first! We need them to exist before they can author a course.
            userRepository.saveAll(List.of(admin, student));

            // --- Create Courses ---
            // Notice we pass 'admin' as the 3rd argument (the author)
            Course javaCourse = new Course(
                    "Java for Beginners",
                    "Learn the basics of Java from scratch.",
                    admin);

            Course springCourse = new Course(
                    "Spring Boot Masterclass",
                    "Build web APIs with Spring Boot.",
                    admin);

            // Save courses
            courseRepository.saveAll(List.of(javaCourse, springCourse));

            System.out.println("✅ Seeding complete! Users and Courses created.");
        } else {
            System.out.println("⚠️ Database already has data, skipping seed.");
        }
    }
}