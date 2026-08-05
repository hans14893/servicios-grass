package com.resergrass.controller;

import com.resergrass.domain.entity.User;
import com.resergrass.dto.UserDto;
import com.resergrass.dto.auth.AuthResponse;
import com.resergrass.dto.auth.LoginRequest;
import com.resergrass.dto.auth.RegisterRequest;
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
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
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
