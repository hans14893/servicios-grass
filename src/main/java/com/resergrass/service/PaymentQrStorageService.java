package com.resergrass.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentQrStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );
    private final Path storageDirectory;

    public PaymentQrStorageService(@Value("${app.uploads.payment-qr-dir:uploads/payment-qr}") String directory) {
        this.storageDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecciona una imagen para el QR");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("La imagen no debe superar los 5 MB");
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) throw new IllegalArgumentException("Solo se permiten imágenes JPG o PNG");
        try {
            try (InputStream input = file.getInputStream()) {
                if (ImageIO.read(input) == null) throw new IllegalArgumentException("El archivo seleccionado no es una imagen válida");
            }
            Files.createDirectories(storageDirectory);
            String filename = UUID.randomUUID() + extension;
            Path destination = storageDirectory.resolve(filename).normalize();
            if (!destination.getParent().equals(storageDirectory)) throw new IllegalArgumentException("Nombre de archivo inválido");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return filename;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar la imagen del QR", exception);
        }
    }

    public Resource load(String filename) {
        if (!filename.matches("[a-fA-F0-9-]+\\.(jpg|png)")) throw new IllegalArgumentException("Imagen inválida");
        try {
            Path file = storageDirectory.resolve(filename).normalize();
            if (!file.getParent().equals(storageDirectory) || !Files.isRegularFile(file)) {
                throw new IllegalArgumentException("La imagen del QR no existe");
            }
            return new UrlResource(file.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer la imagen del QR", exception);
        }
    }
}