package com.orquidea.api.service;

import com.orquidea.api.dto.FichaTaxonomicaDto;
import com.orquidea.api.dto.SolicitudFichaTaxonomica;
import com.orquidea.api.exception.RecursoDuplicadoExcepcion;
import com.orquidea.api.exception.RecursoNoEncontradoExcepcion;
import com.orquidea.api.mapper.FichaTaxonomicaMapper;
import com.orquidea.api.model.FichaTaxonomica;
import com.orquidea.api.repository.FichaTaxonomicaRepositorio;
import com.orquidea.api.storage.AlmacenamientoServicio;
import com.orquidea.api.storage.ValidadorImagen;
import com.orquidea.api.storage.ValidadorImagen.ImagenValidada;
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
public class FichaTaxonomicaServicio {

    private static final String CARPETA_FOTOS = "fichas/";
    private static final String MENSAJE_NO_EXISTE = "La ficha taxonómica solicitada no existe.";

    private final FichaTaxonomicaRepositorio fichaRepositorio;
    private final FichaTaxonomicaMapper fichaMapper;
    private final ValidadorImagen validadorImagen;
    private final AlmacenamientoServicio almacenamientoServicio;

    /** HU-7. Valida todo antes de subir la foto, para no subir archivos por errores del usuario. */
    @Transactional
    public FichaTaxonomicaDto crear(SolicitudFichaTaxonomica solicitud) {
        String nombreCientifico = solicitud.getNombreCientifico().trim();
        if (fichaRepositorio.existsByNombreCientificoIgnoreCase(nombreCientifico)) {
            throw nombreDuplicado(nombreCientifico);
        }
        ImagenValidada foto = validadorImagen.validar(solicitud.getFoto(), SolicitudFichaTaxonomica.MENSAJE_OBLIGATORIOS);

        FichaTaxonomica ficha = fichaMapper.aEntidad(solicitud);
        ficha.setNombreCientifico(nombreCientifico);
        ficha.setFotoClave(subirFoto(foto));
        return aDto(fichaRepositorio.saveAndFlush(ficha));
    }

    /**
     * HU-8. Reemplaza todos los datos de la ficha. La foto es opcional: si no llega (o llega vacía, como
     * envían los navegadores un campo de archivo sin seleccionar) se conserva la actual.
     */
    @Transactional
    public FichaTaxonomicaDto actualizar(UUID id, SolicitudFichaTaxonomica solicitud) {
        FichaTaxonomica ficha = fichaRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(MENSAJE_NO_EXISTE));
        String nombreCientifico = solicitud.getNombreCientifico().trim();
        if (fichaRepositorio.existsByNombreCientificoIgnoreCaseAndIdNot(nombreCientifico, id)) {
            throw nombreDuplicado(nombreCientifico);
        }
        MultipartFile archivo = solicitud.getFoto();
        ImagenValidada fotoNueva = archivo == null || archivo.isEmpty()
                ? null
                : validadorImagen.validar(archivo, SolicitudFichaTaxonomica.MENSAJE_OBLIGATORIOS);

        fichaMapper.actualizar(solicitud, ficha);
        ficha.setNombreCientifico(nombreCientifico);
        if (fotoNueva != null) {
            String fotoAnterior = ficha.getFotoClave();
            ficha.setFotoClave(subirFoto(fotoNueva));
            alConfirmar(() -> almacenamientoServicio.eliminar(fotoAnterior));
        }
        return aDto(fichaRepositorio.saveAndFlush(ficha));
    }

    public FichaTaxonomicaDto obtenerPorId(UUID id) {
        return fichaRepositorio.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(MENSAJE_NO_EXISTE));
    }

    /** Listado completo sin filtros; el listado público por categoría es la HU-10. */
    public List<FichaTaxonomicaDto> listar() {
        return fichaRepositorio.findAll(Sort.by("nombreComun")).stream()
                .map(this::aDto)
                .toList();
    }

    /** Sube la foto y programa su borrado si la transacción no llega a confirmarse. */
    private String subirFoto(ImagenValidada foto) {
        String clave = CARPETA_FOTOS + UUID.randomUUID() + "." + foto.tipo().extension();
        almacenamientoServicio.subir(clave, foto.contenido(), foto.tipo().tipoContenido());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    almacenamientoServicio.eliminar(clave);
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

    private static RecursoDuplicadoExcepcion nombreDuplicado(String nombreCientifico) {
        return new RecursoDuplicadoExcepcion("Ya existe una ficha con el nombre científico '" + nombreCientifico + "'.");
    }

    private FichaTaxonomicaDto aDto(FichaTaxonomica ficha) {
        return fichaMapper.aDto(ficha, almacenamientoServicio.urlPublica(ficha.getFotoClave()));
    }
}
