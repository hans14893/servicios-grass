package com.resergrass.controller;

import com.resergrass.dto.ClientDto;
import com.resergrass.dto.ClientRequest;
import com.resergrass.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public List<ClientDto> all() {
        return clientService.all();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public ClientDto create(@Valid @RequestBody ClientRequest request) {
        return clientService.create(request);
    }
}
