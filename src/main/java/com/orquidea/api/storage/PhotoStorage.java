package com.orquidea.api.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fotos de fichas, estaciones, componentes y reportes: las valida, las optimiza y las guarda con su miniatura.
 * <p>
 * Las fotos viven fuera de la base de datos, así que su limpieza se ata al resultado de la transacción: si se
 * revierte se borran las recién subidas, y las que se reemplazan o eliminan se borran solo cuando se confirma.
 * Por eso subir y eliminar se llaman dentro de una transacción.
 */
@Component
@RequiredArgsConstructor
public class PhotoStorage {

    private static final String CARPETA_MINIATURAS = "miniaturas/";

    private final ImageValidator imageValidator;
    private final ImageProcessor imageProcessor;
    private final StorageService storageService;

    /**
     * Valida y optimiza la foto sin subirla, para revisar toda la solicitud antes de subir nada.
     *
     * @param mensajeSiFalta mensaje cuando el archivo no llega o está vacío (cada HU define el suyo)
     */
    public ProcessedImage preparar(MultipartFile archivo, String mensajeSiFalta) {
        return imageProcessor.procesar(imageValidator.validar(archivo, mensajeSiFalta));
    }

    /** Sube la foto y su miniatura con un nombre nuevo dentro de la carpeta (p. ej. "fichas/"). */
    public StoredPhoto subir(String carpeta, ProcessedImage foto) {
        String clave = carpeta + UUID.randomUUID() + "." + foto.tipo().extension();
        subirConLimpieza(clave, foto.contenido(), foto.tipo());
        return new StoredPhoto(clave, subirMiniatura(clave, foto));
    }

    /** Sube solo la miniatura de una foto ya guardada (al reprocesar una que ya estaba optimizada). */
    public String subirMiniatura(String clave, ProcessedImage foto) {
        String miniaturaClave = miniaturaDe(clave);
        subirConLimpieza(miniaturaClave, foto.miniatura(), foto.tipo());
        return miniaturaClave;
    }

    /** Borra los objetos cuando la transacción se confirme; las claves nulas se ignoran. */
    public void eliminarAlConfirmar(String... claves) {
        List<String> existentes = Arrays.stream(claves).filter(Objects::nonNull).toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                existentes.forEach(storageService::eliminar);
            }
        });
    }

    public byte[] descargar(String clave) {
        return storageService.descargar(clave);
    }

    public String url(String clave) {
        return clave == null ? null : storageService.urlPublica(clave);
    }

    /** Las fotos subidas antes de las miniaturas no tienen una hasta reprocesarlas; mientras, se usa la foto. */
    public String urlMiniatura(String clave, String miniaturaClave) {
        return url(miniaturaClave != null ? miniaturaClave : clave);
    }

    /** fichas/abc.jpg pasa a fichas/miniaturas/abc.jpg */
    static String miniaturaDe(String clave) {
        int inicioNombre = clave.lastIndexOf('/') + 1;
        return clave.substring(0, inicioNombre) + CARPETA_MINIATURAS + clave.substring(inicioNombre);
    }

    /** Sube el objeto y programa su borrado si la transacción no llega a confirmarse. */
    private void subirConLimpieza(String clave, byte[] contenido, ImageType tipo) {
        storageService.subir(clave, contenido, tipo.tipoContenido());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    storageService.eliminar(clave);
                }
            }
        });
    }
}
