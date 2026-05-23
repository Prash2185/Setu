package com.setu.cloudbilling;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Security is function ko use karega database se user nikalne ke liye
    User findByUsername(String username);
}
