package com.opspilot.controlplane.controller;

import com.opspilot.controlplane.dto.auth.AuthResponse;
import com.opspilot.controlplane.dto.auth.LoginRequest;
import com.opspilot.controlplane.dto.auth.RegisterRequest;
import com.opspilot.controlplane.dto.auth.UserResponse;
import com.opspilot.controlplane.dto.common.ApiResponse;
import com.opspilot.controlplane.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Multi-tenant registration, login, and user profile APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register organization and initial admin user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Organization registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user and retrieve JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get currently authenticated user and organization profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
