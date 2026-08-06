package com.resergrass.service;

import com.resergrass.domain.entity.Client;
import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.Role;
import com.resergrass.dto.UserDto;
import com.resergrass.dto.auth.AuthResponse;
import com.resergrass.dto.auth.EmailVerificationRequest;
import com.resergrass.dto.auth.ForgotPasswordRequest;
import com.resergrass.dto.auth.LoginRequest;
import com.resergrass.dto.auth.RegisterRequest;
import com.resergrass.dto.auth.RegistrationResponse;
import com.resergrass.dto.auth.PasswordResetResponse;
import com.resergrass.dto.auth.ResetPasswordRequest;
import com.resergrass.dto.auth.ResendVerificationRequest;
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

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private static final int CODE_EXPIRATION_MINUTES = 10;
    private static final int RESEND_DELAY_SECONDS = 60;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final VerificationEmailService verificationEmailService;

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        var email = normalizeEmail(request.email());
        log.info("AUTH_REGISTER_REQUEST email={}", email);
        if (userRepository.existsByEmail(email)) {
            log.warn("AUTH_REGISTER_DUPLICATED email={}", email);
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        var user = new User();
        user.setFullName(request.fullName().trim().replaceAll("\\s+", " "));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone().trim());
        user.setRole(Role.CLIENTE);
        user.setEnabled(false);
        user.setEmailVerified(false);
        userRepository.save(user);

        var client = new Client();
        client.setUser(user);
        client.setDocumentNumber(request.documentNumber());
        client.setAddress(request.address());
        clientRepository.save(client);

        issueVerificationCode(user);
        log.info("AUTH_REGISTER_PENDING_VERIFICATION userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
        return registrationResponse(user.getEmail(), "Enviamos un código de verificación a tu correo");
    }

    public AuthResponse login(LoginRequest request) {
        var email = normalizeEmail(request.email());
        log.info("AUTH_LOGIN_REQUEST email={}", email);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        log.info("AUTH_LOGIN_SUCCESS userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
        return response(user);
    }

    public AuthResponse verifyEmail(EmailVerificationRequest request) {
        var email = normalizeEmail(request.email());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Código inválido o vencido"));
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya fue verificado");
        }
        var now = OffsetDateTime.now();
        if (user.getEmailVerificationExpiresAt() == null || !now.isBefore(user.getEmailVerificationExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El código venció. Solicita uno nuevo");
        }
        if (user.getEmailVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Superaste el máximo de intentos. Solicita un código nuevo");
        }
        if (!passwordEncoder.matches(request.code(), user.getEmailVerificationCode())) {
            user.setEmailVerificationAttempts(user.getEmailVerificationAttempts() + 1);
            userRepository.save(user);
            var remaining = MAX_VERIFICATION_ATTEMPTS - user.getEmailVerificationAttempts();
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    remaining > 0 ? "Código incorrecto. Te quedan " + remaining + " intentos"
                            : "Superaste el máximo de intentos. Solicita un código nuevo");
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        clearVerification(user);
        userRepository.save(user);
        log.info("AUTH_EMAIL_VERIFIED userId={} email={}", user.getId(), user.getEmail());
        return response(user);
    }

    public RegistrationResponse resendVerification(ResendVerificationRequest request) {
        var email = normalizeEmail(request.email());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No existe una cuenta pendiente con ese correo"));
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya fue verificado");
        }
        var now = OffsetDateTime.now();
        if (user.getEmailVerificationResendAt() != null && now.isBefore(user.getEmailVerificationResendAt())) {
            var seconds = Math.max(1, ChronoUnit.SECONDS.between(now, user.getEmailVerificationResendAt()));
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Podrás reenviar el código en " + seconds + " segundos");
        }
        issueVerificationCode(user);
        log.info("AUTH_VERIFICATION_RESENT userId={} email={}", user.getId(), user.getEmail());
        return registrationResponse(user.getEmail(), "Enviamos un nuevo código de verificación");
    }

    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        var email = normalizeEmail(request.email());
        var genericResponse = passwordResetResponse("Si el correo está registrado, recibirás un código para recuperar tu contraseña");
        var user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.isEnabled() || !user.isEmailVerified()) {
            return genericResponse;
        }
        var now = OffsetDateTime.now();
        if (user.getPasswordResetResendAt() != null && now.isBefore(user.getPasswordResetResendAt())) {
            return genericResponse;
        }
        var code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        user.setPasswordResetCode(passwordEncoder.encode(code));
        user.setPasswordResetExpiresAt(now.plusMinutes(CODE_EXPIRATION_MINUTES));
        user.setPasswordResetResendAt(now.plusSeconds(RESEND_DELAY_SECONDS));
        user.setPasswordResetAttempts(0);
        userRepository.save(user);
        verificationEmailService.sendPasswordResetCode(user.getEmail(), code);
        log.info("AUTH_PASSWORD_RESET_CODE_SENT userId={} email={}", user.getId(), user.getEmail());
        return genericResponse;
    }

    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        var email = normalizeEmail(request.email());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Código inválido o vencido"));
        var now = OffsetDateTime.now();
        if (user.getPasswordResetExpiresAt() == null || !now.isBefore(user.getPasswordResetExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El código venció. Solicita uno nuevo");
        }
        if (user.getPasswordResetAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Superaste el máximo de intentos. Solicita un código nuevo");
        }
        if (!passwordEncoder.matches(request.code(), user.getPasswordResetCode())) {
            user.setPasswordResetAttempts(user.getPasswordResetAttempts() + 1);
            userRepository.save(user);
            var remaining = MAX_VERIFICATION_ATTEMPTS - user.getPasswordResetAttempts();
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    remaining > 0 ? "Código incorrecto. Te quedan " + remaining + " intentos"
                            : "Superaste el máximo de intentos. Solicita un código nuevo");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(now);
        clearPasswordReset(user);
        userRepository.save(user);
        log.info("AUTH_PASSWORD_RESET_SUCCESS userId={} email={}", user.getId(), user.getEmail());
        return new PasswordResetResponse("Tu contraseña fue actualizada. Ya puedes iniciar sesión", 0, 0);
    }

    private void clearPasswordReset(User user) {
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetResendAt(null);
        user.setPasswordResetAttempts(0);
    }

    private PasswordResetResponse passwordResetResponse(String message) {
        return new PasswordResetResponse(message, CODE_EXPIRATION_MINUTES * 60, RESEND_DELAY_SECONDS);
    }

    private void issueVerificationCode(User user) {
        var code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        var now = OffsetDateTime.now();
        user.setEmailVerificationCode(passwordEncoder.encode(code));
        user.setEmailVerificationExpiresAt(now.plusMinutes(CODE_EXPIRATION_MINUTES));
        user.setEmailVerificationResendAt(now.plusSeconds(RESEND_DELAY_SECONDS));
        user.setEmailVerificationAttempts(0);
        userRepository.save(user);
        verificationEmailService.sendVerificationCode(user.getEmail(), code);
    }

    private void clearVerification(User user) {
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        user.setEmailVerificationResendAt(null);
        user.setEmailVerificationAttempts(0);
    }

    private RegistrationResponse registrationResponse(String email, String message) {
        return new RegistrationResponse(email, message, CODE_EXPIRATION_MINUTES * 60, RESEND_DELAY_SECONDS);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.generate(user), user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isEnabled());
    }
}
