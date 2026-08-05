package com.resergrass.service;

import com.resergrass.domain.entity.PaymentSettings;
import com.resergrass.dto.PaymentConfigDto;
import com.resergrass.repository.PaymentSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentConfigService {
    private static final Long SETTINGS_ID = 1L;

    private final PaymentSettingsRepository paymentSettingsRepository;

    @Value("${app.payments.owner-name:Juan Perez}")
    private String ownerName;

    @Value("${app.payments.yape-phone-number:987654321}")
    private String yapePhoneNumber;

    @Value("${app.payments.whatsapp-phone-number:987654321}")
    private String whatsappPhoneNumber;

    @Value("${app.payments.yape-qr-url:https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=YAPE-987654321}")
    private String yapeQrUrl;

    @Value("${app.payments.timeout-minutes:15}")
    private int paymentTimeoutMinutes;

    public PaymentConfigDto config() {
        return paymentSettingsRepository.findById(SETTINGS_ID)
                .map(this::toDto)
                .orElseGet(this::defaultConfig);
    }

    @Transactional
    public PaymentConfigDto update(PaymentConfigDto request) {
        var settings = paymentSettingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
            var created = new PaymentSettings();
            created.setId(SETTINGS_ID);
            return created;
        });
        settings.setOwnerName(required(request.ownerName(), "Debe indicar el nombre del titular"));
        settings.setYapePhoneNumber(required(request.yapePhoneNumber(), "Debe indicar el numero de Yape"));
        settings.setWhatsappPhoneNumber(required(request.whatsappPhoneNumber(), "Debe indicar el numero de WhatsApp"));
        settings.setYapeQrUrl(required(request.yapeQrUrl(), "Debe indicar la URL del QR Yape"));
        settings.setPaymentTimeoutMinutes(request.paymentTimeoutMinutes() > 0 ? request.paymentTimeoutMinutes() : paymentTimeoutMinutes);
        return toDto(paymentSettingsRepository.save(settings));
    }

    public int timeoutMinutes() {
        return config().paymentTimeoutMinutes();
    }

    private PaymentConfigDto defaultConfig() {
        return new PaymentConfigDto(ownerName, yapePhoneNumber, whatsappPhoneNumber, yapeQrUrl, paymentTimeoutMinutes);
    }

    private PaymentConfigDto toDto(PaymentSettings settings) {
        return new PaymentConfigDto(
                settings.getOwnerName(),
                settings.getYapePhoneNumber(),
                settings.getWhatsappPhoneNumber(),
                settings.getYapeQrUrl(),
                settings.getPaymentTimeoutMinutes()
        );
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
