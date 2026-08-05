package com.resergrass.service;

import com.resergrass.domain.entity.Client;
import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.Role;
import com.resergrass.dto.UserDto;
import com.resergrass.dto.auth.AuthResponse;
import com.resergrass.dto.auth.LoginRequest;
import com.resergrass.dto.auth.RegisterRequest;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.ClientRepository;
import com.resergrass.repository.UserRepository;
import com.resergrass.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("AUTH_REGISTER_REQUEST email={}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            log.warn("AUTH_REGISTER_DUPLICATED email={}", request.email());
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        var user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.CLIENTE);
        userRepository.save(user);

        var client = new Client();
        client.setUser(user);
        client.setDocumentNumber(request.documentNumber());
        client.setAddress(request.address());
        clientRepository.save(client);

        log.info("AUTH_REGISTER_SUCCESS userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        var email = request.email().toLowerCase();
        log.info("AUTH_LOGIN_REQUEST email={}", email);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        log.info("AUTH_LOGIN_SUCCESS userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.generate(user), user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isEnabled());
    }
}
