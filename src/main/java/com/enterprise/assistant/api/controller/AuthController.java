package com.enterprise.assistant.api.controller;

import com.enterprise.assistant.api.dto.auth.LoginRequest;
import com.enterprise.assistant.api.dto.auth.LoginResponse;
import com.enterprise.assistant.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT Authentication (Demo: username=admin, password=password123)")
@Profile("!test")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token",
            description = "Authenticate with username/password and receive JWT token. Demo credentials: username=admin, password=password123")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
