package com.setu.cloudbilling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 👇 THE MISSING ENGINE (Password Encryptor) 🔐
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login/**", "/error", "/css/**", "/js/**").permitAll()
                .requestMatchers("/share/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    org.springframework.security.web.savedrequest.SavedRequest savedRequest = (org.springframework.security.web.savedrequest.SavedRequest)
                            request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");

                    if (savedRequest != null) {
                        response.sendRedirect(savedRequest.getRedirectUrl());
                    } else {
                        response.sendRedirect("/dashboard");
                    }
                })
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );
        return http.build();
    }
}