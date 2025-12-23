package com.jeremy.courses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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

    // 3. Map HTTP GET requests to "/users" to this method
    @GetMapping("/users")
    public List<User> getAllUsers() {
        // 4. Fetch all users from the DB and return them as JSON
        return userRepository.findAll();
    }

    // 5. Registration endpoint for students and authors
    @PostMapping("/users/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> registrationData) {
        String name = registrationData.get("name");
        String email = registrationData.get("email");
        String password = registrationData.get("password");
        String role = registrationData.get("role");

        // Validate required fields
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        // Validate role
        if (role == null || role.isEmpty()) {
            role = "STUDENT"; // Default to STUDENT if not specified
        } else if (!role.equals("STUDENT") && !role.equals("CREATOR")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role must be either STUDENT or CREATOR"));
        }

        // Check if email already exists
        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already registered"));
        }

        // Create new user
        User newUser = new User(email, passwordEncoder.encode(password), role);
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
}