package com.orquidea.api.service;

import com.orquidea.api.dto.TaxonDto;
import com.orquidea.api.dto.TaxonRequest;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.TaxonMapper;
import com.orquidea.api.model.Taxon;
import com.orquidea.api.model.TaxonCategory;
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
 * si se revierte, se borra la foto recién subida; si se confirma, se borra la foto que quedó reemplazada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonService {

    private static final String CARPETA_FOTOS = "fichas/";
    private static final String MENSAJE_NO_EXISTE = "La ficha taxonómica solicitada no existe.";

    private final TaxonRepository taxonRepository;
    private final TaxonMapper taxonMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;

    /** HU-7. Valida todo antes de subir la foto, para no subir archivos por errores del usuario. */
    @Transactional
    public TaxonDto crear(TaxonRequest solicitud) {
        String scientificName = solicitud.getScientificName().trim();
        if (taxonRepository.existsByScientificNameIgnoreCase(scientificName)) {
            throw nombreDuplicado(scientificName);
        }
        ValidatedImage foto = imageValidator.validar(solicitud.getFoto(), TaxonRequest.MENSAJE_OBLIGATORIOS);

        Taxon ficha = taxonMapper.aEntidad(solicitud);
        ficha.setScientificName(scientificName);
        ficha.setFotoClave(subirFoto(foto));
        return aDto(taxonRepository.saveAndFlush(ficha));
    }

    /**
     * HU-8. Reemplaza todos los datos de la ficha. La foto es opcional: si no llega (o llega vacía, como
     * envían los navegadores un campo de archivo sin seleccionar) se conserva la actual.
     */
    @Transactional
    public TaxonDto actualizar(UUID id, TaxonRequest solicitud) {
        Taxon ficha = taxonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
        String scientificName = solicitud.getScientificName().trim();
        if (taxonRepository.existsByScientificNameIgnoreCaseAndIdNot(scientificName, id)) {
            throw nombreDuplicado(scientificName);
        }
        MultipartFile archivo = solicitud.getFoto();
        ValidatedImage fotoNueva = archivo == null || archivo.isEmpty()
                ? null
                : imageValidator.validar(archivo, TaxonRequest.MENSAJE_OBLIGATORIOS);

        taxonMapper.actualizar(solicitud, ficha);
        ficha.setScientificName(scientificName);
        if (fotoNueva != null) {
            String fotoAnterior = ficha.getFotoClave();
            ficha.setFotoClave(subirFoto(fotoNueva));
            alConfirmar(() -> storageService.eliminar(fotoAnterior));
        }
        return aDto(taxonRepository.saveAndFlush(ficha));
    }

    public TaxonDto obtenerPorId(UUID id) {
        return taxonRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
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

    private TaxonDto aDto(Taxon ficha) {
        return taxonMapper.aDto(ficha, storageService.urlPublica(ficha.getFotoClave()));
    }
}
