package com.setu.cloudbilling;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements Filter {

    // 🧠 Map jo har user (ya IP) ke liye alag se token bucket yaad rakhega
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // 🎁 Bucket Rule: 1 minute mein max 20 requests allow hain
    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        // 🎯 Sirf heavy endpoints par rate limiting lagao (Dashboard, Upload, Download)
        if (path.startsWith("/dashboard") || path.startsWith("/upload") || path.startsWith("/download")) {
            
            // User ka naam nikalne ki koshish karo, nahi toh IP address pakdo
            String key = httpRequest.getRemoteAddr(); 
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                key = auth.getName();
            }

            // Agar user ka bucket pehle se nahi hai, toh naya banao
            Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket());

            // 🛑 Token consume karne ki koshish karo, agar token nahi bacha toh block karo!
            if (!bucket.tryConsume(1)) {
                httpResponse.setStatus(429); // HTTP 429 Too Many Requests
                httpResponse.setContentType("text/html");
                httpResponse.getWriter().write(
                    "<div style='font-family: sans-serif; text-align:center; padding: 50px; background: #fff5f5; height: 100vh;'>" +
                    "<h1 style='color:#dc3545; font-size: 40px; margin-bottom: 20px;'>🚨 Rate Limit Exceeded!</h1>" +
                    "<h3 style='color:#333;'>Bhai, thoda dheere! Shanti rakho... 🛑</h3>" +
                    "<p style='color:#666; font-size: 16px;'>You have crossed the safety limit of <b>20 requests per minute</b>.</p>" +
                    "<p style='color:#999;'>This feature protects our cloud infrastructure from DDoS attacks.</p>" +
                    "<br><a href='/dashboard' style='padding: 12px 25px; background: #dc3545; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;'>Try Again After 1 Minute</a>" +
                    "</div>"
                );
                return; // Chain aage nahi badhegi, request yahin block!
            }
        }

        // Agar sab theek hai toh request aage jaane do
        chain.doFilter(request, response);
    }
}