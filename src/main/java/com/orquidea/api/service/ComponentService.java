package com.orquidea.api.service;

import com.orquidea.api.config.ComponentProperties;
import com.orquidea.api.dto.ComponentDto;
import com.orquidea.api.dto.ComponentPhotoDto;
import com.orquidea.api.dto.ComponentUpdateRequest;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.ComponentMapper;
import com.orquidea.api.model.ComponentType;
import com.orquidea.api.model.WetlandComponent;
import com.orquidea.api.model.WetlandComponentPhoto;
import com.orquidea.api.repository.ComponentRepository;
import com.orquidea.api.storage.ImageValidator;
import com.orquidea.api.storage.ImageValidator.ValidatedImage;
import com.orquidea.api.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * HU-14 (consulta) y HU-15 (gestión). Igual que en las fichas, las fotos viven fuera de la base de datos:
 * si la transacción se revierte se borra la foto recién subida; si se confirma, se borra la eliminada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentService {

    private static final String CARPETA_FOTOS = "componentes/";
    private static final String MENSAJE_NO_EXISTE = "El componente solicitado no existe.";
    private static final String MENSAJE_FOTO_NO_EXISTE = "La foto solicitada no existe en este componente.";
    private static final String MENSAJE_FOTO_OBLIGATORIA = "Debes seleccionar una imagen.";

    private final ComponentRepository componentRepository;
    private final ComponentMapper componentMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;
    private final ComponentProperties propiedades;

    /** Siempre los mismos componentes, en el orden del enum; los que no tienen información vienen marcados. */
    public List<ComponentDto> listar() {
        return componentRepository.findAll().stream()
                .sorted(Comparator.comparing(WetlandComponent::getTipo))
                .map(this::aDto)
                .toList();
    }

    public ComponentDto obtener(ComponentType tipo) {
        return aDto(buscar(tipo));
    }

    /** HU-15, escenario 1: reemplaza nombre y descripción. */
    @Transactional
    public ComponentDto actualizar(ComponentType tipo, ComponentUpdateRequest solicitud) {
        WetlandComponent componente = buscar(tipo);
        componentMapper.actualizar(solicitud, componente);
        componente.setNombre(solicitud.getNombre().trim());
        componente.setDescripcion(solicitud.getDescripcion().trim());
        componente.setFechaActualizacion(Instant.now());
        return aDto(componentRepository.saveAndFlush(componente));
    }

    /** HU-15: agrega una foto (jpg o png, máx. 10 MB) si el componente no llegó al máximo de fotos. */
    @Transactional
    public ComponentDto agregarFoto(ComponentType tipo, MultipartFile archivo) {
        WetlandComponent componente = buscar(tipo);
        if (componente.getFotos().size() >= propiedades.fotosMaximas()) {
            throw new OperationNotAllowedException(
                    "El componente ya tiene el máximo de " + propiedades.fotosMaximas() + " fotos.");
        }
        ValidatedImage foto = imageValidator.validar(archivo, MENSAJE_FOTO_OBLIGATORIA);

        componente.getFotos().add(WetlandComponentPhoto.builder()
                .componente(componente)
                .fotoClave(subirFoto(foto))
                .build());
        componente.setFechaActualizacion(Instant.now());
        return aDto(componentRepository.saveAndFlush(componente));
    }

    /** HU-15: elimina una foto del componente y la borra del almacenamiento. */
    @Transactional
    public void eliminarFoto(ComponentType tipo, UUID idFoto) {
        WetlandComponent componente = buscar(tipo);
        WetlandComponentPhoto foto = componente.getFotos().stream()
                .filter(f -> f.getId().equals(idFoto))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_FOTO_NO_EXISTE));
        componente.getFotos().remove(foto);
        componente.setFechaActualizacion(Instant.now());
        componentRepository.saveAndFlush(componente);
        alConfirmar(() -> storageService.eliminar(foto.getFotoClave()));
    }

    private WetlandComponent buscar(ComponentType tipo) {
        return componentRepository.findById(tipo)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
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

    private ComponentDto aDto(WetlandComponent componente) {
        List<ComponentPhotoDto> fotos = componente.getFotos().stream()
                .map(foto -> componentMapper.aFotoDto(foto, storageService.urlPublica(foto.getFotoClave())))
                .toList();
        boolean tieneContenido = !fotos.isEmpty()
                || (componente.getDescripcion() != null && !componente.getDescripcion().isBlank());
        return componentMapper.aDto(componente, fotos, tieneContenido);
    }
}
