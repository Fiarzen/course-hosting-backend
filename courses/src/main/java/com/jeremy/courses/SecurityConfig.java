package com.jeremy.courses;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // <--- Import this
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/courses", // Public courses listing
                                "/users/register", // Registration endpoint
                                "/files/**", // Serve uploaded files (e.g., PDFs) without auth; access to URLs is controlled by lesson APIs
                                "/v3/api-docs", // Exact match for the JSON
                                "/v3/api-docs/**", // Match for sub-groups (if you have them)
                                "/swagger-ui/**", // The UI static resources (CSS/JS)
                                "/swagger-ui.html" // The UI index page
                        )
                        .permitAll() // Keep the listed endpoints public
                        .anyRequest().authenticated() // Everything else requires login
                )
                .httpBasic(withDefaults());

        return http.build();
    }
}