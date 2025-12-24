package com.jeremy.courses;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final PasswordEncoder passwordEncoder;

    // 2. Inject it in the constructor
    public DataSeeder(UserRepository u, CourseRepository c, LessonRepository l, PasswordEncoder pe) {
        this.userRepository = u;
        this.courseRepository = c;
        this.lessonRepository = l;
        this.passwordEncoder = pe;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            System.out.println("🌱 Seeding database...");

            User admin = new User("admin@example.com", passwordEncoder.encode("pass123"), "ADMIN");
            admin.setName("Admin User");

            User student = new User("student@example.com", passwordEncoder.encode("pass321"), "STUDENT");
            student.setName("Jeremy Student");

            userRepository.saveAll(List.of(admin, student));

            Course javaCourse = new Course("Java for Beginners", "Basics of Java", admin);
            Course springCourse = new Course("Spring Boot Masterclass", "Build APIs", admin);

            courseRepository.saveAll(List.of(javaCourse, springCourse));

            // --- 3. Create Lessons ---
            // We pass "null" for the PDF if there isn't one, or a fake URL if there is.

            Lesson l1 = new Lesson(
                    "Intro to Java",
                    "History of Java...",
                    "video1.mp4",
                    "http://files.example.com/intro_notes.pdf", // <--- PDF URL
                    javaCourse);

            Lesson l2 = new Lesson(
                    "Variables",
                    "Int, String, Boolean...",
                    "video2.mp4",
                    null, // <--- No PDF for this lesson
                    javaCourse);

            Lesson l3 = new Lesson(
                    "Loops",
                    "For and While loops...",
                    "video3.mp4",
                    "http://files.example.com/loop_cheat_sheet.pdf", // <--- PDF URL
                    javaCourse);

            lessonRepository.saveAll(List.of(l1, l2, l3));

            System.out.println("✅ Seeding complete! Users, Courses, and Lessons created.");
        }
    }
}