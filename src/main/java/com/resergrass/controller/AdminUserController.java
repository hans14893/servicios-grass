package com.resergrass.controller;

import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.Role;
import com.resergrass.dto.AdminUserRequest;
import com.resergrass.dto.AdminUserUpdateRequest;
import com.resergrass.dto.UserDto;
import com.resergrass.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody AdminUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request,
                          @AuthenticationPrincipal User actor) {
        return userService.update(id, request, actor);
    }
    @PatchMapping("/{id}/role")
    public UserDto updateRole(@PathVariable Long id, @RequestParam Role role, @AuthenticationPrincipal User actor) {
        return userService.updateRole(id, role, actor);
    }

    @PatchMapping("/{id}/enabled")
    public UserDto updateEnabled(@PathVariable Long id, @RequestParam boolean enabled, @AuthenticationPrincipal User actor) {
        return userService.updateEnabled(id, enabled, actor);
    }
}