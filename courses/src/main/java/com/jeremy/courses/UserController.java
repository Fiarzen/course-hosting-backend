package com.jeremy.courses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController // 1. Tells Spring this class handles web requests
public class UserController {

    private final UserRepository userRepository;

    // 2. Inject the repository so the controller can access the database
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 3. Map HTTP GET requests to "/users" to this method
    @GetMapping("/users")
    public List<User> getAllUsers() {
        // 4. Fetch all users from the DB and return them as JSON
        return userRepository.findAll();
    }
}