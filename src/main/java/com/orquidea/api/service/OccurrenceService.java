package com.orquidea.api.service;

import com.orquidea.api.dto.OccurrenceDto;
import com.orquidea.api.dto.OccurrenceRequest;
import com.orquidea.api.dto.PageResponse;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.exception.InvalidRequestException;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.OccurrenceMapper;
import com.orquidea.api.model.Kingdom;
import com.orquidea.api.model.Occurrence;
import com.orquidea.api.model.ReviewStatus;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.OccurrenceRepository;
import com.orquidea.api.repository.UserRepository;
import com.orquidea.api.storage.ImageValidator;
import com.orquidea.api.storage.ImageValidator.ValidatedImage;
import com.orquidea.api.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

/**
 * Reportes ciudadanos de avistamiento: creación (HU-18), revisión (HU-19) y foro público (HU-20).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccurrenceService {

    public static final String DESCONOCIDO = "Desconocido";
    public static final int TAMANO_MAXIMO_PAGINA = 50;

    private static final String CARPETA_FOTOS = "reportes/";
    private static final String MENSAJE_NO_EXISTE = "El reporte solicitado no existe.";
    private static final String MENSAJE_YA_REVISADO = "El reporte ya fue revisado.";

    private final OccurrenceRepository occurrenceRepository;
    private final UserRepository userRepository;
    private final OccurrenceMapper occurrenceMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;

    /** Resultado de crear: el reporte y si es nuevo o ya existía (reenvío con el mismo id). */
    public record CreationResult(OccurrenceDto reporte, boolean nuevo) {
    }

    /**
     * HU-18. Solo la descripción es obligatoria; reino y clasificación sin valor quedan como desconocidos
     * (escenario 3) y el autor es siempre el usuario autenticado (escenario 4). Si el cliente reenvía un id
     * que ya registró, se devuelve el reporte existente en lugar de crear otro.
     */
    @Transactional
    public CreationResult crear(OccurrenceRequest solicitud, UUID idAutor) {
        User autor = userRepository.findById(idAutor)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario autenticado no existe."));
        if (solicitud.getId() != null) {
            Optional<Occurrence> existente = occurrenceRepository.findById(solicitud.getId());
            if (existente.isPresent()) {
                if (!existente.get().getRecordedBy().getId().equals(idAutor)) {
                    throw new DuplicateResourceException("Ya existe un reporte con ese identificador.");
                }
                return new CreationResult(aDto(existente.get()), false);
            }
        }
        if ((solicitud.getDecimalLatitude() == null) != (solicitud.getDecimalLongitude() == null)) {
            throw new InvalidRequestException(OccurrenceRequest.MENSAJE_COORDENADAS);
        }
        MultipartFile archivo = solicitud.getFoto();
        ValidatedImage foto = archivo == null || archivo.isEmpty() ? null : imageValidator.validar(archivo, null);

        Occurrence reporte = Occurrence.builder()
                .id(solicitud.getId() != null ? solicitud.getId() : UUID.randomUUID())
                .occurrenceRemarks(solicitud.getOccurrenceRemarks().trim())
                .kingdom(solicitud.getKingdom() != null ? solicitud.getKingdom() : Kingdom.DESCONOCIDO)
                .vernacularName(textoODesconocido(solicitud.getVernacularName()))
                .decimalLatitude(solicitud.getDecimalLatitude())
                .decimalLongitude(solicitud.getDecimalLongitude())
                .estado(ReviewStatus.PENDIENTE)
                .recordedBy(autor)
                .build();
        if (foto != null) {
            reporte.setFotoClave(subirFoto(foto));
        }
        return new CreationResult(aDto(occurrenceRepository.saveAndFlush(reporte)), true);
    }

    /** HU-19, escenario 3: cola de revisión, los más antiguos primero. */
    public PageResponse<OccurrenceDto> listarPendientes(int pagina, int tamano) {
        return listar(ReviewStatus.PENDIENTE, Sort.by("fechaCreacion").ascending(), pagina, tamano);
    }

    /** HU-20: foro público, solo aprobados y los más recientes primero. */
    public PageResponse<OccurrenceDto> listarAprobados(int pagina, int tamano) {
        return listar(ReviewStatus.APROBADO, Sort.by("fechaRevision").descending(), pagina, tamano);
    }

    /** HU-19, escenario 1: el reporte pasa al foro público. */
    @Transactional
    public OccurrenceDto aprobar(UUID id, UUID idRevisor) {
        return revisar(id, idRevisor, ReviewStatus.APROBADO);
    }

    /** HU-19, escenario 2: el reporte sale de la cola y no se publica. */
    @Transactional
    public OccurrenceDto rechazar(UUID id, UUID idRevisor) {
        return revisar(id, idRevisor, ReviewStatus.RECHAZADO);
    }

    private OccurrenceDto revisar(UUID id, UUID idRevisor, ReviewStatus nuevoEstado) {
        Occurrence reporte = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
        if (reporte.getEstado() != ReviewStatus.PENDIENTE) {
            throw new OperationNotAllowedException(MENSAJE_YA_REVISADO);
        }
        reporte.revisar(nuevoEstado, userRepository.getReferenceById(idRevisor));
        return aDto(occurrenceRepository.saveAndFlush(reporte));
    }

    private PageResponse<OccurrenceDto> listar(ReviewStatus estado, Sort orden, int pagina, int tamano) {
        PageRequest paginacion = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA),
                orden.and(Sort.by("id")));
        return PageResponse.de(occurrenceRepository.findByEstado(estado, paginacion), this::aDto);
    }

    /** Sube la foto y programa su borrado si la transacción no llega a confirmarse. */
    private String subirFoto(ValidatedImage foto) {
        String clave = CARPETA_FOTOS + UUID.randomUUID() + "." + foto.tipo().extension();
        storageService.subir(clave, foto.contenido(), foto.tipo().tipoContenido());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    storageService.eliminar(clave);
                }
            }
        });
        return clave;
    }

    private static String textoODesconocido(String valor) {
        return valor == null || valor.isBlank() ? DESCONOCIDO : valor.trim();
    }

    private OccurrenceDto aDto(Occurrence reporte) {
        String urlFoto = reporte.getFotoClave() == null ? null : storageService.urlPublica(reporte.getFotoClave());
        return occurrenceMapper.aDto(reporte, urlFoto);
    }
}
