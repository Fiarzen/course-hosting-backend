package com.jeremy.courses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController // 1. Tells Spring this class handles web requests
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 2. Inject the repository and password encoder so the controller can access the database
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 3. Map HTTP GET requests to "/users" to this method - ADMIN only
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        // Check if user has ADMIN role
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can access this endpoint"));
        }

        // Return all users
        return ResponseEntity.ok(userRepository.findAll());
    }

    // 5. Registration endpoint - all new users are STUDENTS
    @PostMapping("/users/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> registrationData) {
        String name = registrationData.get("name");
        String email = registrationData.get("email");
        String password = registrationData.get("password");

        // Validate required fields
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        // Check if email already exists
        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already registered"));
        }

        // Create new user - always STUDENT role
        User newUser = new User(email, passwordEncoder.encode(password), "STUDENT");
        if (name != null && !name.isEmpty()) {
            newUser.setName(name);
        }

        // Save user to database
        User savedUser = userRepository.save(newUser);

        // Return user without password
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // 6. Get current authenticated user
    @GetMapping("/users/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        // Get email from authentication principal
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        // Return user without password
        return ResponseEntity.ok(user);
    }

    // 7. Admin endpoint to upgrade a user to CREATOR role
    @PostMapping("/users/{userId}/upgrade-to-creator")
    public ResponseEntity<?> upgradeUserToCreator(@PathVariable Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        // Check if requester is ADMIN
        String email = authentication.getName();
        User admin = userRepository.findByEmail(email);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can upgrade users"));
        }

        // Find the user to upgrade
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        if ("CREATOR".equals(user.getRole()) || "ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User is already a CREATOR or ADMIN"));
        }

        user.setRole("CREATOR");
        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User successfully upgraded to CREATOR",
                "user", updatedUser
        ));
    }
}