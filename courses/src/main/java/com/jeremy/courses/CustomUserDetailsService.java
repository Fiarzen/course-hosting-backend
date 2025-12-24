package com.jeremy.courses;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Find user in DB by email
        User user = userRepository.findByEmail(email);
        // Note: You might need to add `findByEmail` to your UserRepository interface!

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        // 2. Convert to Spring Security's "UserDetails" object
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // This must be the Hashed password from DB
                .roles(user.getRole())
                .build();
    }
}