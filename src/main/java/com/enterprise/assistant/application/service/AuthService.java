package com.enterprise.assistant.application.service;

import com.enterprise.assistant.api.dto.auth.LoginRequest;
import com.enterprise.assistant.api.dto.auth.LoginResponse;
import com.enterprise.assistant.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!test")
public class AuthService {

    private final JwtUtil jwtUtil;

    @Value("${app.auth.username}")
    private String validUsername;

    @Value("${app.auth.password}")
    private String validPassword;

    public LoginResponse login(LoginRequest request) {
        if (!validUsername.equals(request.getUsername()) ||
                !validPassword.equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(request.getUsername());
        log.info("User logged in: {}", request.getUsername());

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(request.getUsername())
                .build();
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }
}
