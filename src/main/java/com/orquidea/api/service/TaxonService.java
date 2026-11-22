package com.orquidea.api.service;

import com.orquidea.api.config.TaxonProperties;
import com.orquidea.api.dto.PageResponse;
import com.orquidea.api.dto.TaxonDto;
import com.orquidea.api.dto.TaxonPhotoDto;
import com.orquidea.api.dto.TaxonRequest;
import com.orquidea.api.dto.TaxonSyncResponse;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.exception.InvalidRequestException;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.TaxonMapper;
import com.orquidea.api.model.Taxon;
import com.orquidea.api.model.TaxonCategory;
import com.orquidea.api.model.TaxonPhoto;
import com.orquidea.api.repository.DeletedTaxonRepository;
import com.orquidea.api.repository.TaxonRepository;
import com.orquidea.api.storage.PhotoStorage;
import com.orquidea.api.storage.ProcessedImage;
import com.orquidea.api.storage.StoredPhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Las fotos viven fuera de la base de datos; PhotoStorage ata su limpieza al resultado de la transacción.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonService {

    public static final int TAMANO_MAXIMO_PAGINA = 50;

    private static final Sort ORDEN_ALFABETICO = Sort.by("vernacularName", "id");
    private static final Sort ORDEN_SINCRONIZACION = Sort.by("fechaActualizacion", "id");
    /** Cuánto se repasa hacia atrás al sincronizar; ver sincronizar(). */
    private static final Duration MARGEN_SINCRONIZACION = Duration.ofMinutes(1);
    private static final String CARPETA_FOTOS = "fichas/";
    private static final String MENSAJE_NO_EXISTE = "La ficha taxonómica solicitada no existe.";
    private static final String MENSAJE_FOTO_NO_EXISTE = "La foto solicitada no existe en esta ficha.";
    private static final String MENSAJE_FOTO_OBLIGATORIA = "Debes seleccionar una imagen.";

    private final TaxonRepository taxonRepository;
    private final DeletedTaxonRepository deletedTaxonRepository;
    private final TaxonMapper taxonMapper;
    private final PhotoStorage photoStorage;
    private final TaxonProperties propiedades;

    /**
     * HU-7. Valida todo antes de subir las fotos, para no subir archivos por errores del usuario. Los campos de
     * archivo vacíos (así envían los navegadores uno sin seleccionar) no cuentan como foto.
     */
    @Transactional
    public TaxonDto crear(TaxonRequest solicitud) {
        String scientificName = solicitud.getScientificName().trim();
        if (taxonRepository.existsByScientificNameIgnoreCase(scientificName)) {
            throw nombreDuplicado(scientificName);
        }
        List<MultipartFile> archivos = solicitud.getFotos() == null ? List.of()
                : solicitud.getFotos().stream().filter(archivo -> archivo != null && !archivo.isEmpty()).toList();
        if (archivos.isEmpty()) {
            throw new InvalidRequestException(MENSAJE_FOTO_OBLIGATORIA);
        }
        if (archivos.size() > propiedades.fotosMaximas()) {
            throw maximoDeFotos();
        }
        List<ProcessedImage> fotos = archivos.stream()
                .map(archivo -> photoStorage.preparar(archivo, MENSAJE_FOTO_OBLIGATORIA))
                .toList();

        Taxon ficha = taxonMapper.aEntidad(solicitud);
        ficha.setScientificName(scientificName);
        fotos.forEach(foto -> agregar(ficha, foto));
        return aDto(taxonRepository.saveAndFlush(ficha));
    }

    /** HU-8. Reemplaza todos los datos de texto de la ficha; las fotos tienen sus propios endpoints. */
    @Transactional
    public TaxonDto actualizar(UUID id, TaxonRequest solicitud) {
        Taxon ficha = buscar(id);
        String scientificName = solicitud.getScientificName().trim();
        if (taxonRepository.existsByScientificNameIgnoreCaseAndIdNot(scientificName, id)) {
            throw nombreDuplicado(scientificName);
        }
        taxonMapper.actualizar(solicitud, ficha);
        ficha.setScientificName(scientificName);
        return aDto(taxonRepository.saveAndFlush(ficha));
    }

    /** HU-7 (ajuste): agrega una foto al final (jpg o png, máx. 10 MB) si la ficha no llegó al máximo. */
    @Transactional
    public TaxonDto agregarFoto(UUID id, MultipartFile archivo) {
        Taxon ficha = buscar(id);
        if (ficha.getFotos().size() >= propiedades.fotosMaximas()) {
            throw maximoDeFotos();
        }
        agregar(ficha, photoStorage.preparar(archivo, MENSAJE_FOTO_OBLIGATORIA));
        ficha.setFechaActualizacion(Instant.now());
        return aDto(taxonRepository.saveAndFlush(ficha));
    }

    /**
     * HU-7 (ajuste): elimina una foto y la borra del almacenamiento. Si era la principal, la siguiente pasa a
     * serlo. La ficha conserva siempre al menos una foto.
     */
    @Transactional
    public void eliminarFoto(UUID id, UUID idFoto) {
        Taxon ficha = buscar(id);
        TaxonPhoto foto = ficha.getFotos().stream()
                .filter(f -> f.getId().equals(idFoto))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_FOTO_NO_EXISTE));
        if (ficha.getFotos().size() == 1) {
            throw new OperationNotAllowedException("La ficha debe conservar al menos una foto.");
        }
        ficha.getFotos().remove(foto);
        ficha.setFechaActualizacion(Instant.now());
        taxonRepository.saveAndFlush(ficha);
        photoStorage.eliminarAlConfirmar(foto.getFotoClave(), foto.getMiniaturaClave());
    }

    public TaxonDto obtenerPorId(UUID id) {
        return aDto(buscar(id));
    }

    /**
     * HU-10 y HU-21. Pagina en la base de datos: por nombre común, en el orden alfabético español que da la
     * intercalación de la columna (V16), y por id para que el orden sea estable entre páginas.
     */
    public PageResponse<TaxonDto> listar(TaxonCategory categoria, int pagina, int tamano) {
        PageRequest paginacion = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA),
                ORDEN_ALFABETICO);
        Page<Taxon> fichas = categoria == null
                ? taxonRepository.findAll(paginacion)
                : taxonRepository.findByCategoria(categoria, paginacion);
        return PageResponse.de(fichas, this::aDto);
    }

    /**
     * HU-21. Sin fecha devuelve todas las fichas; con fecha, las creadas o modificadas desde entonces y los ids de
     * las eliminadas.
     * <p>
     * La fecha de modificación se asigna antes de confirmar la transacción: una edición que se confirma justo
     * después de esta lectura puede quedar con una fecha anterior a fechaSincronizacion, y el cliente no la vería
     * nunca. Por eso se repasa un margen hacia atrás; repetir una ficha no hace daño porque el cliente la guarda por
     * id. REPEATABLE_READ hace que las dos consultas vean el mismo estado, así una ficha borrada entre ambas no
     * aparece a la vez en las dos listas.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TaxonSyncResponse sincronizar(Instant modificadoDesde) {
        Instant fechaSincronizacion = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (modificadoDesde == null) {
            return new TaxonSyncResponse(fechaSincronizacion, true,
                    aDtos(taxonRepository.findAll(ORDEN_SINCRONIZACION)), List.of());
        }
        Instant desde = acotar(modificadoDesde, fechaSincronizacion).minus(MARGEN_SINCRONIZACION);
        return new TaxonSyncResponse(fechaSincronizacion, false,
                aDtos(taxonRepository.findByFechaActualizacionGreaterThanEqual(desde, ORDEN_SINCRONIZACION)),
                deletedTaxonRepository.idsEliminadosDesde(desde));
    }

    /** Una fecha anterior a 1970 o futura no viene del servidor; se acota para que la consulta no falle. */
    private static Instant acotar(Instant fecha, Instant ahora) {
        if (fecha.isBefore(Instant.EPOCH)) {
            return Instant.EPOCH;
        }
        return fecha.isAfter(ahora) ? ahora : fecha;
    }

    private Taxon buscar(UUID id) {
        return taxonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
    }

    /** Agrega la foto después de la última; las posiciones pueden tener huecos tras eliminar fotos. */
    private void agregar(Taxon ficha, ProcessedImage foto) {
        int posicion = ficha.getFotos().stream().mapToInt(TaxonPhoto::getPosicion).max().orElse(-1) + 1;
        StoredPhoto guardada = photoStorage.subir(CARPETA_FOTOS, foto);
        ficha.getFotos().add(TaxonPhoto.builder()
                .ficha(ficha)
                .fotoClave(guardada.clave())
                .miniaturaClave(guardada.miniaturaClave())
                .posicion(posicion)
                .build());
    }

    private static DuplicateResourceException nombreDuplicado(String scientificName) {
        return new DuplicateResourceException("Ya existe una ficha con el nombre científico '" + scientificName + "'.");
    }

    private OperationNotAllowedException maximoDeFotos() {
        return new OperationNotAllowedException(
                "La ficha ya tiene el máximo de " + propiedades.fotosMaximas() + " fotos.");
    }

    private List<TaxonDto> aDtos(List<Taxon> fichas) {
        return fichas.stream().map(this::aDto).toList();
    }

    private TaxonDto aDto(Taxon ficha) {
        List<TaxonPhotoDto> fotos = ficha.getFotos().stream()
                .map(foto -> taxonMapper.aFotoDto(foto, photoStorage.url(foto.getFotoClave()),
                        photoStorage.urlMiniatura(foto.getFotoClave(), foto.getMiniaturaClave())))
                .toList();
        return taxonMapper.aDto(ficha, fotos);
    }
}
