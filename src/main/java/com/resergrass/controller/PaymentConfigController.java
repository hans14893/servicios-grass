package com.resergrass.controller;

import com.resergrass.dto.FileUploadDto;
import com.resergrass.dto.PaymentConfigDto;
import com.resergrass.service.PaymentConfigService;
import com.resergrass.service.PaymentQrStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/payment-config")
@RequiredArgsConstructor
public class PaymentConfigController {
    private final PaymentConfigService paymentConfigService;
    private final PaymentQrStorageService paymentQrStorageService;

    @GetMapping
    public PaymentConfigDto config() {
        return paymentConfigService.config();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentConfigDto update(@RequestBody PaymentConfigDto request) {
        return paymentConfigService.update(request);
    }

    @PostMapping(value = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public FileUploadDto uploadQr(@RequestPart("file") MultipartFile file) {
        String filename = paymentQrStorageService.store(file);
        return new FileUploadDto("/api/payment-config/qr/" + filename);
    }

    @GetMapping("/qr/{filename:.+}")
    public ResponseEntity<Resource> qrImage(@PathVariable String filename) {
        Resource resource = paymentQrStorageService.load(filename);
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .contentType(type)
                .body(resource);
    }
}