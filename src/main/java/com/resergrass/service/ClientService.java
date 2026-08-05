package com.resergrass.service;

import com.resergrass.domain.entity.Client;
import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.Role;
import com.resergrass.dto.ClientDto;
import com.resergrass.dto.ClientRequest;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.ClientRepository;
import com.resergrass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ClientDto> all() {
        var clients = clientRepository.findAllByOrderByUserFullNameAsc().stream().map(this::toDto).toList();
        log.info("CLIENT_LIST count={}", clients.size());
        return clients;
    }

    @Transactional
    public ClientDto create(ClientRequest request) {
        var email = normalizeEmail(request);
        log.info("CLIENT_CREATE_REQUEST fullName={} email={}", request.fullName(), email);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        var user = new User();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setRole(Role.CLIENTE);
        user.setPassword(passwordEncoder.encode("cliente123"));
        userRepository.save(user);

        var client = new Client();
        client.setUser(user);
        client.setDocumentNumber(request.documentNumber());
        client.setAddress(request.address());
        var saved = clientRepository.save(client);
        log.info("CLIENT_CREATE_SUCCESS clientId={} userId={} email={}", saved.getId(), user.getId(), user.getEmail());
        return toDto(saved);
    }

    private String normalizeEmail(ClientRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            return request.email().trim().toLowerCase();
        }
        return "cliente+" + System.currentTimeMillis() + "@resergrass.local";
    }

    private ClientDto toDto(Client client) {
        var user = client.getUser();
        return new ClientDto(
                client.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                client.getDocumentNumber(),
                client.getAddress()
        );
    }
}
