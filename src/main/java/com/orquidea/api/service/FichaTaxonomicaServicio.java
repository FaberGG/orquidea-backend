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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FichaTaxonomicaServicio {

    private static final String CARPETA_FOTOS = "fichas/";

    private final FichaTaxonomicaRepositorio fichaRepositorio;
    private final FichaTaxonomicaMapper fichaMapper;
    private final ValidadorImagen validadorImagen;
    private final AlmacenamientoServicio almacenamientoServicio;

    /**
     * Valida todo antes de subir la foto, para no dejar archivos huérfanos por errores del usuario.
     * Si falla el guardado después de subirla, la foto se elimina.
     */
    @Transactional
    public FichaTaxonomicaDto crear(SolicitudFichaTaxonomica solicitud) {
        String nombreCientifico = solicitud.getNombreCientifico().trim();
        if (fichaRepositorio.existsByNombreCientificoIgnoreCase(nombreCientifico)) {
            throw new RecursoDuplicadoExcepcion(
                    "Ya existe una ficha con el nombre científico '" + nombreCientifico + "'.");
        }
        ImagenValidada foto = validadorImagen.validar(solicitud.getFoto(), SolicitudFichaTaxonomica.MENSAJE_OBLIGATORIOS);

        FichaTaxonomica ficha = fichaMapper.aEntidad(solicitud);
        ficha.setNombreCientifico(nombreCientifico);
        ficha.setFotoClave(CARPETA_FOTOS + UUID.randomUUID() + "." + foto.tipo().extension());

        almacenamientoServicio.subir(ficha.getFotoClave(), foto.contenido(), foto.tipo().tipoContenido());
        try {
            return aDto(fichaRepositorio.saveAndFlush(ficha));
        } catch (RuntimeException e) {
            almacenamientoServicio.eliminar(ficha.getFotoClave());
            throw e;
        }
    }

    public FichaTaxonomicaDto obtenerPorId(UUID id) {
        return fichaRepositorio.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("La ficha taxonómica solicitada no existe."));
    }

    /** Listado completo sin filtros; el listado público por categoría es la HU-10. */
    public List<FichaTaxonomicaDto> listar() {
        return fichaRepositorio.findAll(Sort.by("nombreComun")).stream()
                .map(this::aDto)
                .toList();
    }

    private FichaTaxonomicaDto aDto(FichaTaxonomica ficha) {
        return fichaMapper.aDto(ficha, almacenamientoServicio.urlPublica(ficha.getFotoClave()));
    }
}
