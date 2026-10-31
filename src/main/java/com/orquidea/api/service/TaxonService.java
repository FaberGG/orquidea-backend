package com.orquidea.api.service;

import com.orquidea.api.config.TaxonProperties;
import com.orquidea.api.dto.TaxonDto;
import com.orquidea.api.dto.TaxonPhotoDto;
import com.orquidea.api.dto.TaxonRequest;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.exception.InvalidRequestException;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.TaxonMapper;
import com.orquidea.api.model.Taxon;
import com.orquidea.api.model.TaxonCategory;
import com.orquidea.api.model.TaxonPhoto;
import com.orquidea.api.repository.TaxonRepository;
import com.orquidea.api.storage.StorageService;
import com.orquidea.api.storage.ImageValidator;
import com.orquidea.api.storage.ImageValidator.ValidatedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Las fotos viven fuera de la base de datos, así que su limpieza se ata al resultado de la transacción:
 * si se revierte, se borran las fotos recién subidas; si se confirma, se borra la foto eliminada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonService {

    private static final String CARPETA_FOTOS = "fichas/";
    private static final String MENSAJE_NO_EXISTE = "La ficha taxonómica solicitada no existe.";
    private static final String MENSAJE_FOTO_NO_EXISTE = "La foto solicitada no existe en esta ficha.";
    private static final String MENSAJE_FOTO_OBLIGATORIA = "Debes seleccionar una imagen.";

    private final TaxonRepository taxonRepository;
    private final TaxonMapper taxonMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;
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
        List<ValidatedImage> fotos = archivos.stream()
                .map(archivo -> imageValidator.validar(archivo, MENSAJE_FOTO_OBLIGATORIA))
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

    /** HU-7 (v2.0): agrega una foto al final (jpg o png, máx. 10 MB) si la ficha no llegó al máximo. */
    @Transactional
    public TaxonDto agregarFoto(UUID id, MultipartFile archivo) {
        Taxon ficha = buscar(id);
        if (ficha.getFotos().size() >= propiedades.fotosMaximas()) {
            throw maximoDeFotos();
        }
        agregar(ficha, imageValidator.validar(archivo, MENSAJE_FOTO_OBLIGATORIA));
        return aDto(taxonRepository.saveAndFlush(ficha));
    }

    /**
     * HU-7 (v2.0): elimina una foto y la borra del almacenamiento. Si era la principal, la siguiente pasa a
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
        taxonRepository.saveAndFlush(ficha);
        alConfirmar(() -> storageService.eliminar(foto.getFotoClave()));
    }

    public TaxonDto obtenerPorId(UUID id) {
        return aDto(buscar(id));
    }

    /** Listado completo sin filtros; el listado público por categoría es la HU-10. */
    public List<TaxonDto> listar(TaxonCategory categoria) {
        Sort order = Sort.by("vernacularName");
        List<Taxon> fichas = categoria == null
            ? taxonRepository.findAll(order)
            : taxonRepository.findByCategoria(categoria, order);
        return fichas.stream()
            .map(this::aDto)
            .toList();
    }

    private Taxon buscar(UUID id) {
        return taxonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
    }

    /** Agrega la foto después de la última; las posiciones pueden tener huecos tras eliminar fotos. */
    private void agregar(Taxon ficha, ValidatedImage foto) {
        int posicion = ficha.getFotos().stream().mapToInt(TaxonPhoto::getPosicion).max().orElse(-1) + 1;
        ficha.getFotos().add(TaxonPhoto.builder()
                .ficha(ficha)
                .fotoClave(subirFoto(foto))
                .posicion(posicion)
                .build());
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

    private static DuplicateResourceException nombreDuplicado(String scientificName) {
        return new DuplicateResourceException("Ya existe una ficha con el nombre científico '" + scientificName + "'.");
    }

    private OperationNotAllowedException maximoDeFotos() {
        return new OperationNotAllowedException(
                "La ficha ya tiene el máximo de " + propiedades.fotosMaximas() + " fotos.");
    }

    private TaxonDto aDto(Taxon ficha) {
        List<TaxonPhotoDto> fotos = ficha.getFotos().stream()
                .map(foto -> taxonMapper.aFotoDto(foto, storageService.urlPublica(foto.getFotoClave())))
                .toList();
        return taxonMapper.aDto(ficha, fotos);
    }
}
