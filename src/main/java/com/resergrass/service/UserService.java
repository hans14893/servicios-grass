package com.resergrass.service;

import com.resergrass.domain.entity.Client;
import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.Role;
import com.resergrass.dto.AdminUserRequest;
import com.resergrass.dto.AdminUserUpdateRequest;
import com.resergrass.dto.UserDto;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.ClientRepository;
import com.resergrass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public UserDto create(AdminUserRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede crear otro administrador desde esta opción");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        var user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        var saved = userRepository.save(user);
        if (saved.getRole() == Role.CLIENTE) {
            var client = new Client();
            client.setUser(saved);
            clientRepository.save(client);
        }
        return toDto(saved);
    }

    @Transactional
    public UserDto update(Long id, AdminUserUpdateRequest request, User actor) {
        var user = find(id);
        String previousEmail = user.getEmail();
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado"); });
        if (request.role() == Role.ADMIN && user.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede asignar el rol administrador desde esta opción");
        }
        if (user.getId().equals(actor.getId()) && request.role() != user.getRole()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes cambiar tu propio rol");
        }
        if (request.role() == Role.CLIENTE && clientRepository.findByUserEmail(previousEmail).isEmpty()) {
            var client = new Client();
            client.setUser(user);
            clientRepository.save(client);
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return toDto(userRepository.save(user));
    }
    @Transactional
    public UserDto updateRole(Long id, Role role, User actor) {
        var user = find(id);
        if (user.getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes cambiar tu propio rol");
        }
        if (role == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede asignar el rol administrador desde esta opción");
        }
        if (role == Role.CLIENTE && clientRepository.findByUserEmail(user.getEmail()).isEmpty()) {
            var client = new Client();
            client.setUser(user);
            clientRepository.save(client);
        }
        user.setRole(role);
        return toDto(userRepository.save(user));
    }

    public UserDto updateEnabled(Long id, boolean enabled, User actor) {
        var user = find(id);
        if (user.getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta");
        }
        user.setEnabled(enabled);
        return toDto(userRepository.save(user));
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isEnabled());
    }
}