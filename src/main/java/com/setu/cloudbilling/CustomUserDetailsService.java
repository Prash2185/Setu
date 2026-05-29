package com.setu.cloudbilling;

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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Database se user dhundho (email used as username)
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Bhai, yeh user exist nahi karta!"));

        String pwd = user.getPassword() == null ? "" : user.getPassword();

        // Spring Security ko uska user format bana kar do
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(pwd)
            .roles(user.getRole().replace("ROLE_", ""))
            .build();
    }
}