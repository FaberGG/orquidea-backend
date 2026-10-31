package com.orquidea.api.service;

import com.orquidea.api.dto.StationDto;
import com.orquidea.api.dto.StationRequest;
import com.orquidea.api.dto.StationTaxonDto;
import com.orquidea.api.exception.InvalidRequestException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.StationMapper;
import com.orquidea.api.model.Station;
import com.orquidea.api.model.Taxon;
import com.orquidea.api.repository.StationRepository;
import com.orquidea.api.repository.TaxonRepository;
import com.orquidea.api.storage.ImageValidator;
import com.orquidea.api.storage.ImageValidator.ValidatedImage;
import com.orquidea.api.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * HU-13. Igual que en las fichas, la foto vive fuera de la base de datos: si la transacción se revierte se borra
 * la foto recién subida; si se confirma, se borra la que quedó reemplazada o la de la estación eliminada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationService {

    private static final String CARPETA_FOTOS = "estaciones/";
    private static final String MENSAJE_NO_EXISTE = "La estación no existe.";
    private static final String MENSAJE_FICHA_NO_EXISTE = "Alguna de las fichas asociadas no existe.";
    /** Decimales que guarda la base de datos (NUMERIC(9, 6)), unos 10 cm de precisión. */
    private static final int DECIMALES_COORDENADAS = 6;
    /** Alfabético en español: sin distinguir mayúsculas ni tildes ("Águila" antes que "Azulejo"). */
    private static final Collator ORDEN_ALFABETICO = Collator.getInstance(Locale.forLanguageTag("es"));

    private final StationRepository stationRepository;
    private final TaxonRepository taxonRepository;
    private final StationMapper stationMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;

    /** Ordenadas por nombre y sin paginar: el mapa las muestra todas. */
    public List<StationDto> listar() {
        return stationRepository.findAll(Sort.by("nombre")).stream()
                .map(this::aDto)
                .toList();
    }

    public StationDto obtenerPorId(UUID id) {
        return aDto(buscar(id));
    }

    /** CA1 y CA4. Valida todo antes de subir la foto, para no subir archivos por errores del usuario. */
    @Transactional
    public StationDto crear(StationRequest solicitud) {
        ValidatedImage foto = imageValidator.validar(solicitud.getFoto(), StationRequest.MENSAJE_OBLIGATORIOS);
        List<Taxon> fichas = buscarFichas(solicitud.getFichaIds());

        Station estacion = stationMapper.aEntidad(solicitud);
        copiarDatos(solicitud, estacion, fichas);
        estacion.setFotoClave(subirFoto(foto));
        return aDto(stationRepository.saveAndFlush(estacion));
    }

    /**
     * CA2. Reemplaza los datos y las fichas asociadas. La foto es opcional: si no llega (o llega vacía, como
     * envían los navegadores un campo de archivo sin seleccionar) se conserva la actual.
     */
    @Transactional
    public StationDto actualizar(UUID id, StationRequest solicitud) {
        Station estacion = buscar(id);
        MultipartFile archivo = solicitud.getFoto();
        ValidatedImage fotoNueva = archivo == null || archivo.isEmpty()
                ? null
                : imageValidator.validar(archivo, StationRequest.MENSAJE_OBLIGATORIOS);
        List<Taxon> fichas = buscarFichas(solicitud.getFichaIds());

        stationMapper.actualizar(solicitud, estacion);
        copiarDatos(solicitud, estacion, fichas);
        if (fotoNueva != null) {
            String fotoAnterior = estacion.getFotoClave();
            estacion.setFotoClave(subirFoto(fotoNueva));
            alConfirmar(() -> storageService.eliminar(fotoAnterior));
        }
        return aDto(stationRepository.saveAndFlush(estacion));
    }

    /** CA5. Las asociaciones con fichas se borran con la estación; las fichas no cambian. */
    @Transactional
    public void eliminar(UUID id) {
        Station estacion = buscar(id);
        stationRepository.delete(estacion);
        stationRepository.flush();
        alConfirmar(() -> storageService.eliminar(estacion.getFotoClave()));
    }

    private Station buscar(UUID id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
    }

    /** Los ids repetidos cuentan una sola vez; basta con que uno no exista para rechazar la solicitud. */
    private List<Taxon> buscarFichas(List<UUID> fichaIds) {
        if (fichaIds == null || fichaIds.isEmpty()) {
            return List.of();
        }
        List<UUID> distintos = fichaIds.stream().distinct().toList();
        List<Taxon> fichas = taxonRepository.findAllById(distintos);
        if (fichas.size() != distintos.size()) {
            throw new InvalidRequestException(MENSAJE_FICHA_NO_EXISTE);
        }
        return fichas;
    }

    /**
     * Las coordenadas se redondean a los decimales que guarda la base de datos, para que la respuesta coincida
     * con lo que devolverá una consulta posterior.
     */
    private static void copiarDatos(StationRequest solicitud, Station estacion, List<Taxon> fichas) {
        estacion.setNombre(solicitud.getNombre().trim());
        estacion.setDescripcion(solicitud.getDescripcion().trim());
        estacion.setDecimalLatitude(redondear(solicitud.getDecimalLatitude()));
        estacion.setDecimalLongitude(redondear(solicitud.getDecimalLongitude()));
        estacion.getFichas().clear();
        estacion.getFichas().addAll(fichas);
    }

    private static BigDecimal redondear(BigDecimal coordenada) {
        return coordenada.setScale(DECIMALES_COORDENADAS, RoundingMode.HALF_UP);
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

    private static void alConfirmar(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }

    /** Las fichas van por nombre común; se ordenan aquí para que la respuesta al guardar coincida con la consulta. */
    private StationDto aDto(Station estacion) {
        List<StationTaxonDto> fichas = estacion.getFichas().stream()
                .sorted(Comparator.comparing(Taxon::getVernacularName, ORDEN_ALFABETICO))
                .map(ficha -> stationMapper.aFichaDto(ficha, storageService.urlPublica(ficha.getFotoClave())))
                .toList();
        return stationMapper.aDto(estacion, storageService.urlPublica(estacion.getFotoClave()), fichas);
    }
}
