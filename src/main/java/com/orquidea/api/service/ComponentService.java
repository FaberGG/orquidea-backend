package com.orquidea.api.service;

import com.orquidea.api.config.ComponentProperties;
import com.orquidea.api.dto.ComponentDto;
import com.orquidea.api.dto.ComponentPhotoDto;
import com.orquidea.api.dto.ComponentUpdateRequest;
import com.orquidea.api.dto.ComponentCreateRequest;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.exception.InvalidRequestException;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.ComponentMapper;
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
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.text.Normalizer;
import java.util.Locale;

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
    private static final String MENSAJE_NOMBRE_DUPLICADO = "Ya existe un componente con ese nombre.";
    private static final String MENSAJE_NOMBRE_INVALIDO = "El nombre del componente debe contener letras o números.";
    private static final int LONGITUD_MAXIMA_CODIGO = 100;

    private final ComponentRepository componentRepository;
    private final ComponentMapper componentMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;
    private final ComponentProperties propiedades;

    /** Siempre los mismos componentes, en el orden del enum; los que no tienen información vienen marcados. */
    /** Todos los componentes existentes, en su orden de presentación; los que no tienen información vienen marcados. */
    public List<ComponentDto> listar() {
        return componentRepository.findAll(Sort.by("orden")).stream()
            .map(this::aDto)
            .toList();
    }

    public ComponentDto obtener(String tipo) {
        return aDto(buscar(tipo));
    }

    /**
     * HU-25. El código se genera a partir del nombre (p. ej. "Componente hídrico" pasa a COMPONENTE_HIDRICO).
     * Dos nombres que producen el mismo código (p. ej. "Hídrico" e "Hidrico") cuentan como duplicados.
     */
    @Transactional
    public ComponentDto crear(ComponentCreateRequest solicitud) {
        String nombre = solicitud.getNombre().trim();
        String tipo = generarCodigo(nombre);
        if (componentRepository.existsByNombreIgnoreCase(nombre) || componentRepository.existsById(tipo)) {
            throw new DuplicateResourceException(MENSAJE_NOMBRE_DUPLICADO);
        }
        WetlandComponent componente = WetlandComponent.builder()
            .tipo(tipo)
            .nombre(nombre)
            .descripcion(solicitud.getDescripcion().trim())
            .orden(componentRepository.maximoOrden() + 1)
            .fechaActualizacion(Instant.now().truncatedTo(ChronoUnit.MICROS))
            .build();
        return aDto(componentRepository.saveAndFlush(componente));
    }

    /** HU-15, escenario 1: reemplaza nombre y descripción. */
    @Transactional
    public ComponentDto actualizar(String tipo, ComponentUpdateRequest solicitud) {
        WetlandComponent componente = buscar(tipo);
        String nombre = solicitud.getNombre().trim();
        if (componentRepository.existsByNombreIgnoreCaseAndTipoNot(nombre, tipo)) {
            throw new DuplicateResourceException(MENSAJE_NOMBRE_DUPLICADO);
        }
        componentMapper.actualizar(solicitud, componente);
        componente.setNombre(nombre);
        componentMapper.actualizar(solicitud, componente);
        componente.setNombre(solicitud.getNombre().trim());
        componente.setDescripcion(solicitud.getDescripcion().trim());
        componente.setFechaActualizacion(Instant.now());
        return aDto(componentRepository.saveAndFlush(componente));
    }

    /** HU-15: agrega una foto (jpg o png, máx. 10 MB) si el componente no llegó al máximo de fotos. */
    @Transactional
    public ComponentDto agregarFoto(String tipo, MultipartFile archivo) {
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
    public void eliminarFoto(String tipo, UUID idFoto) {
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

    /** Mayúsculas sin tildes, con "_" en lugar de espacios y signos: "Componente hídrico" pasa a COMPONENTE_HIDRICO. */
    private static String generarCodigo(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String codigo = sinTildes.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (codigo.length() > LONGITUD_MAXIMA_CODIGO) {
            codigo = codigo.substring(0, LONGITUD_MAXIMA_CODIGO).replaceAll("_+$", "");
        }
        if (codigo.isEmpty()) {
            throw new InvalidRequestException(MENSAJE_NOMBRE_INVALIDO);
        }
        return codigo;
    }
    
    private WetlandComponent buscar(String tipo) {
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
