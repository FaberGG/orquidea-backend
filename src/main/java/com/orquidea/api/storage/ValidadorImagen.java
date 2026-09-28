package com.orquidea.api.storage;

import com.orquidea.api.config.PropiedadesStorage;
import com.orquidea.api.exception.FormatoImagenInvalidoExcepcion;
import com.orquidea.api.exception.ImagenDemasiadoGrandeExcepcion;
import com.orquidea.api.exception.SolicitudInvalidaExcepcion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
@RequiredArgsConstructor
public class ValidadorImagen {

    private final PropiedadesStorage propiedades;

    /** Contenido de una imagen ya validada, listo para subir. */
    public record ImagenValidada(byte[] contenido, TipoImagen tipo) {
    }

    /**
     * @param mensajeSiFalta mensaje cuando el archivo no llega o está vacío (cada HU define el suyo)
     */
    public ImagenValidada validar(MultipartFile archivo, String mensajeSiFalta) {
        if (archivo == null || archivo.isEmpty()) {
            throw new SolicitudInvalidaExcepcion(mensajeSiFalta);
        }
        if (archivo.getSize() > propiedades.tamanoMaximoImagen().toBytes()) {
            throw new ImagenDemasiadoGrandeExcepcion(propiedades.tamanoMaximoImagen());
        }
        byte[] contenido = leer(archivo);
        TipoImagen tipo = TipoImagen.detectar(contenido).orElseThrow(FormatoImagenInvalidoExcepcion::new);
        return new ImagenValidada(contenido, tipo);
    }

    private static byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo recibido", e);
        }
    }
}
