package com.enterprise.assistant.application.service;

import com.enterprise.assistant.api.dto.LoginRequest;
import com.enterprise.assistant.api.dto.LoginResponse;
import com.enterprise.assistant.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

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

        return LoginResponse.builder()
                .token(jwtUtil.generateToken(request.getUsername()))
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
