package com.orquidea.api.service;

import com.orquidea.api.exception.InvalidImageFormatException;
import com.orquidea.api.model.Occurrence;
import com.orquidea.api.model.Station;
import com.orquidea.api.model.TaxonPhoto;
import com.orquidea.api.model.WetlandComponentPhoto;
import com.orquidea.api.repository.OccurrenceRepository;
import com.orquidea.api.repository.StationRepository;
import com.orquidea.api.repository.TaxonPhotoRepository;
import com.orquidea.api.repository.WetlandComponentPhotoRepository;
import com.orquidea.api.storage.ImageProcessor;
import com.orquidea.api.storage.ImageType;
import com.orquidea.api.storage.ImageValidator.ValidatedImage;
import com.orquidea.api.storage.PhotoStorage;
import com.orquidea.api.storage.ProcessedImage;
import com.orquidea.api.storage.StoredPhoto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Optimiza las fotos subidas antes de que existiera la optimización y les crea su miniatura. Lo activa
 * STORAGE_REPROCESAR_FOTOS (ver docs/api/procesamiento-de-fotos.md).
 * <p>
 * Cada foto va en su propia transacción, y un error en una no detiene las demás. Si la foto cambia, se guarda con
 * una clave nueva y la anterior se borra al confirmar: nunca hay un registro apuntando a un objeto a medio
 * escribir. Las fichas y componentes afectados cambian su fecha de modificación; así la app sin conexión (HU-21)
 * recibe las URLs nuevas en la siguiente sincronización.
 * <p>
 * Es idempotente: solo toma las fotos sin miniatura, y al terminar todas la tienen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoReprocessingService {

    private static final double BYTES_POR_MB = 1024 * 1024;

    private final TaxonPhotoRepository taxonPhotoRepository;
    private final StationRepository stationRepository;
    private final WetlandComponentPhotoRepository componentPhotoRepository;
    private final OccurrenceRepository occurrenceRepository;
    private final ImageProcessor imageProcessor;
    private final PhotoStorage photoStorage;
    private final TransactionTemplate transacciones;

    /**
     * @param pendientes   fotos sin miniatura al empezar
     * @param procesadas   optimizadas (o que se optimizarían, al simular)
     * @param fallidas     no se pudieron descargar, leer o guardar; quedan pendientes
     * @param omitidas     se reemplazaron o eliminaron mientras se procesaban
     * @param bytesAntes   peso original de las procesadas
     * @param bytesDespues peso optimizado de las procesadas
     */
    public record ReprocessingSummary(int pendientes, int procesadas, int fallidas, int omitidas,
                                      long bytesAntes, long bytesDespues) {
    }

    /**
     * Una foto pendiente y cómo guardarle las claves nuevas. La asignación corre dentro de la transacción y
     * devuelve false si la foto ya no tiene la clave que se procesó.
     */
    private record Pending(String clave, Predicate<StoredPhoto> asignar) {
    }

    @Async
    public void reprocesarEnSegundoPlano(boolean aplicar) {
        reprocesar(aplicar);
    }

    /** @param aplicar false solo calcula el ahorro, sin escribir nada */
    public ReprocessingSummary reprocesar(boolean aplicar) {
        List<Pending> pendientes = buscarPendientes();
        log.info("Reprocesando {} fotos{}", pendientes.size(), aplicar ? "" : " (simulación)");
        int procesadas = 0;
        int fallidas = 0;
        int omitidas = 0;
        long bytesAntes = 0;
        long bytesDespues = 0;
        for (Pending pendiente : pendientes) {
            byte[] original;
            ProcessedImage optimizada;
            try {
                original = photoStorage.descargar(pendiente.clave());
                ImageType tipo = ImageType.detectar(original).orElseThrow(InvalidImageFormatException::new);
                optimizada = imageProcessor.procesar(new ValidatedImage(original, tipo));
            } catch (RuntimeException e) {
                fallidas++;
                log.warn("No se pudo reprocesar la foto {}: {}", pendiente.clave(), e.getMessage());
                continue;
            }
            if (aplicar) {
                boolean cambia = !Arrays.equals(original, optimizada.contenido());
                try {
                    if (!Boolean.TRUE.equals(transacciones.execute(estado -> guardar(pendiente, optimizada, cambia,
                            estado)))) {
                        omitidas++;
                        continue;
                    }
                } catch (RuntimeException e) {
                    fallidas++;
                    log.warn("No se pudo guardar la foto reprocesada {}: {}", pendiente.clave(), e.getMessage());
                    continue;
                }
            }
            procesadas++;
            bytesAntes += original.length;
            bytesDespues += optimizada.contenido().length;
        }
        ReprocessingSummary resumen = new ReprocessingSummary(pendientes.size(), procesadas, fallidas, omitidas,
                bytesAntes, bytesDespues);
        log.info("Reprocesamiento de fotos terminado{}: {} procesadas, {} fallidas, {} omitidas; {} pasan a {} ({} menos)",
                aplicar ? "" : " (simulación, no se cambió nada)", procesadas, fallidas, omitidas,
                megabytes(bytesAntes), megabytes(bytesDespues), porcentajeAhorrado(bytesAntes, bytesDespues));
        return resumen;
    }

    /**
     * Si la foto no cambia (ya estaba optimizada y sin metadatos), solo se le agrega la miniatura. Lo subido se
     * borra solo si la transacción se revierte (PhotoStorage).
     */
    private boolean guardar(Pending pendiente, ProcessedImage optimizada, boolean cambia, TransactionStatus estado) {
        String clave = pendiente.clave();
        StoredPhoto nueva = cambia
                ? photoStorage.subir(clave.substring(0, clave.lastIndexOf('/') + 1), optimizada)
                : new StoredPhoto(clave, photoStorage.subirMiniatura(clave, optimizada));
        if (!pendiente.asignar().test(nueva)) {
            estado.setRollbackOnly();
            return false;
        }
        if (cambia) {
            photoStorage.eliminarAlConfirmar(clave);
        }
        return true;
    }

    private List<Pending> buscarPendientes() {
        List<Pending> pendientes = new ArrayList<>();
        taxonPhotoRepository.findByMiniaturaClaveIsNull().forEach(foto -> pendientes.add(new Pending(
                foto.getFotoClave(), nueva -> asignarFotoDeFicha(foto.getId(), foto.getFotoClave(), nueva))));
        stationRepository.findByMiniaturaClaveIsNull().forEach(estacion -> pendientes.add(new Pending(
                estacion.getFotoClave(), nueva -> asignarFotoDeEstacion(estacion.getId(), estacion.getFotoClave(),
                nueva))));
        componentPhotoRepository.findByMiniaturaClaveIsNull().forEach(foto -> pendientes.add(new Pending(
                foto.getFotoClave(), nueva -> asignarFotoDeComponente(foto.getId(), foto.getFotoClave(), nueva))));
        occurrenceRepository.findByFotoClaveIsNotNullAndMiniaturaClaveIsNull().forEach(reporte -> pendientes.add(
                new Pending(reporte.getFotoClave(), nueva -> asignarFotoDeReporte(reporte.getId(),
                        reporte.getFotoClave(), nueva))));
        return pendientes;
    }

    /** La fecha de la ficha cambia para que la app sin conexión descargue las URLs nuevas (HU-21). */
    private boolean asignarFotoDeFicha(UUID id, String claveProcesada, StoredPhoto nueva) {
        TaxonPhoto foto = taxonPhotoRepository.findById(id).orElse(null);
        if (foto == null || !claveProcesada.equals(foto.getFotoClave())) {
            return false;
        }
        foto.setFotoClave(nueva.clave());
        foto.setMiniaturaClave(nueva.miniaturaClave());
        foto.getFicha().setFechaActualizacion(Instant.now());
        return true;
    }

    /** La estación actualiza su fecha sola (@PreUpdate). */
    private boolean asignarFotoDeEstacion(UUID id, String claveProcesada, StoredPhoto nueva) {
        Station estacion = stationRepository.findById(id).orElse(null);
        if (estacion == null || !claveProcesada.equals(estacion.getFotoClave())) {
            return false;
        }
        estacion.setFotoClave(nueva.clave());
        estacion.setMiniaturaClave(nueva.miniaturaClave());
        return true;
    }

    private boolean asignarFotoDeComponente(UUID id, String claveProcesada, StoredPhoto nueva) {
        WetlandComponentPhoto foto = componentPhotoRepository.findById(id).orElse(null);
        if (foto == null || !claveProcesada.equals(foto.getFotoClave())) {
            return false;
        }
        foto.setFotoClave(nueva.clave());
        foto.setMiniaturaClave(nueva.miniaturaClave());
        foto.getComponente().setFechaActualizacion(Instant.now());
        return true;
    }

    private boolean asignarFotoDeReporte(UUID id, String claveProcesada, StoredPhoto nueva) {
        Occurrence reporte = occurrenceRepository.findById(id).orElse(null);
        if (reporte == null || !claveProcesada.equals(reporte.getFotoClave())) {
            return false;
        }
        reporte.setFotoClave(nueva.clave());
        reporte.setMiniaturaClave(nueva.miniaturaClave());
        return true;
    }

    private static String megabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f MB", bytes / BYTES_POR_MB);
    }

    private static String porcentajeAhorrado(long antes, long despues) {
        return antes == 0 ? "0 %" : String.format(Locale.ROOT, "%.0f %%", 100.0 * (antes - despues) / antes);
    }
}
