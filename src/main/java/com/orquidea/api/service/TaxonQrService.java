package com.orquidea.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/** Crea el QR descargable para una ficha taxonómica existente. */
@Service
public class TaxonQrService {

    private static final int DIMENSION = 1024;

    public byte[] generar(UUID id) {
        String contenido = "orquideamobileapp://species/" + id;
        try {
            var matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, DIMENSION, DIMENSION,
                    Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                            EncodeHintType.MARGIN, 2));
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (WriterException | IOException excepcion) {
            throw new IllegalStateException("No fue posible generar el código QR de la ficha.", excepcion);
        }
    }
}
