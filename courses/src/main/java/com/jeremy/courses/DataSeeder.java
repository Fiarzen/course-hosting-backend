package com.jeremy.courses;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component // This tells Spring to find this class and run it automatically
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    // Spring "Injects" your repository here automatically
    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the database is empty
        if (userRepository.count() == 0) {
            System.out.println("🌱 Seeding database with initial users...");

            User admin = new User("admin@example.com", "password123", "CREATOR");
            User student = new User("student@example.com", "pass321", "STUDENT");

            userRepository.saveAll(List.of(admin, student));

            System.out.println("✅ Seeding complete! Added " + userRepository.count() + " users.");
        } else {
            System.out.println("⚠️ Database already has data, skipping seed.");
        }
    }
}