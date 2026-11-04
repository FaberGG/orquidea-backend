package com.orquidea.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.orquidea.api.dto.AnnouncementRequest;
import com.orquidea.api.dto.AnnouncementResponse;
import com.orquidea.api.dto.AnnouncementUpdateRequest;
import com.orquidea.api.mapper.AnnouncementMapper;
import com.orquidea.api.model.Announcement;
import com.orquidea.api.model.Estado;
import com.orquidea.api.repository.AnnouncementRepository;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    @Transactional
    public AnnouncementResponse ingresarAnuncio(AnnouncementRequest request) {
        Announcement guardado = announcementRepository.save(announcementMapper.aEntidad(request));
        return announcementMapper.aDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> obtenerAnuncios() {
        return announcementMapper.aDtos(announcementRepository.findVisibles(java.time.Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> obtenerAnunciosAdministracion() {
        return announcementMapper.aDtos(announcementRepository.findAllByOrderByFechaCreacionDesc());
    }


    @Transactional
    public AnnouncementResponse actualizarAnuncio(UUID id, AnnouncementUpdateRequest request) {
        Announcement anuncio = buscar(id);
        announcementMapper.actualizarEntidad(request, anuncio);
        return announcementMapper.aDto(anuncio);
    }

    @Transactional
    public AnnouncementResponse ocultarAnuncio(UUID id) {
        Announcement anuncio = buscar(id);
        anuncio.setEstado(Estado.INACTIVO);
        return announcementMapper.aDto(anuncio);
    }

    @Transactional
    public AnnouncementResponse mostrarAnuncio(UUID id) {
        Announcement anuncio = buscar(id);

        boolean expirado = anuncio.getFechaExpiracion() != null
                && !anuncio.getFechaExpiracion().isAfter(Instant.now());
        if (expirado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes mostrar un anuncio que ya expiró.");
        }

        anuncio.setEstado(Estado.ACTIVO);
        return announcementMapper.aDto(anuncio);
    }

    private Announcement buscar(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El anuncio no existe."));
    }

}
