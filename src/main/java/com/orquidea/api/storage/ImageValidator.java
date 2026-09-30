package com.orquidea.api.storage;

import com.orquidea.api.config.StorageProperties;
import com.orquidea.api.exception.InvalidImageFormatException;
import com.orquidea.api.exception.ImageTooLargeException;
import com.orquidea.api.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
@RequiredArgsConstructor
public class ImageValidator {

    private final StorageProperties propiedades;

    /** Contenido de una imagen ya validada, listo para subir. */
    public record ValidatedImage(byte[] contenido, ImageType tipo) {
    }

    /**
     * @param mensajeSiFalta mensaje cuando el archivo no llega o está vacío (cada HU define el suyo)
     */
    public ValidatedImage validar(MultipartFile archivo, String mensajeSiFalta) {
        if (archivo == null || archivo.isEmpty()) {
            throw new InvalidRequestException(mensajeSiFalta);
        }
        if (archivo.getSize() > propiedades.tamanoMaximoImagen().toBytes()) {
            throw new ImageTooLargeException(propiedades.tamanoMaximoImagen());
        }
        byte[] contenido = leer(archivo);
        ImageType tipo = ImageType.detectar(contenido).orElseThrow(InvalidImageFormatException::new);
        return new ValidatedImage(contenido, tipo);
    }

    private static byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo recibido", e);
        }
    }
}
