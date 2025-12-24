package com.jeremy.courses;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByAuthToken(String authToken);
    User findByPasswordResetToken(String passwordResetToken);
}
