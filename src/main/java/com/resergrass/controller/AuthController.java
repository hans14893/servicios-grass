package com.resergrass.controller;

import com.resergrass.domain.entity.User;
import com.resergrass.dto.UserDto;
import com.resergrass.dto.auth.AuthResponse;
import com.resergrass.dto.auth.EmailVerificationRequest;
import com.resergrass.dto.auth.LoginRequest;
import com.resergrass.dto.auth.RegisterRequest;
import com.resergrass.dto.auth.RegistrationResponse;
import com.resergrass.dto.auth.ResendVerificationRequest;
import com.resergrass.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public RegistrationResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return authService.resendVerification(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        return authService.toDto(user);
    }
}
