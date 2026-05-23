package com.setu.cloudbilling;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // MySQL mein 'users' naam ki table banegi
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // Yeh user ka email/id hoga

    @Column(nullable = false)
    private String password;

    private String role = "ROLE_USER"; // Admin aur User ko alag karne ke liye

    // Getters and Setters (Tu apne IDE se generate kar lena ya manually likh lena)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}