package com.resergrass.service;

import com.resergrass.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class VerificationEmailService {
    private final RestClient restClient;
    private final String apiKey;
    private final String from;

    public VerificationEmailService(
            @Value("${app.email.resend-api-key:}") String apiKey,
            @Value("${app.email.from:}") String from
    ) {
        this.restClient = RestClient.builder().baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey;
        this.from = from;
    }

    public void sendVerificationCode(String email, String code) {
        send(email, "Código de verificación de ReserGrass", emailHtml(
                "Usa este código para verificar tu cuenta:", code,
                "Si no solicitaste esta cuenta, ignora este correo."));
    }

    public void sendPasswordResetCode(String email, String code) {
        send(email, "Recupera tu contraseña de ReserGrass", emailHtml(
                "Usa este código para crear una nueva contraseña:", code,
                "Si no solicitaste cambiar tu contraseña, ignora este correo."));
    }

    private void send(String email, String subject, String html) {
        if (apiKey.isBlank() || from.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El envío de correos no está configurado");
        }
        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "ReserGrass/1.0")
                    .body(Map.of(
                            "from", from,
                            "to", new String[]{email},
                            "subject", subject,
                            "html", html
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo enviar el código de verificación");
        }
    }

    private String emailHtml(String instruction, String code, String footer) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;color:#14201c">
                  <h1 style="color:#36a832">ReserGrass</h1>
                  <p>%s</p>
                  <div style="font-size:34px;font-weight:700;letter-spacing:8px;padding:18px;background:#f1f7ef;text-align:center;border-radius:10px">%s</div>
                  <p>El código vence en 10 minutos.</p>
                  <p style="color:#68736e;font-size:13px">%s</p>
                </div>
                """.formatted(instruction, code, footer);
    }
}
