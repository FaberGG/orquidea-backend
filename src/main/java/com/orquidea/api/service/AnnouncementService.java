package com.orquidea.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.orquidea.api.dto.AnnouncementRequest;
import com.orquidea.api.dto.AnnouncementResponse;
import com.orquidea.api.mapper.AnnouncementMapper;
import com.orquidea.api.model.Announcement;
import com.orquidea.api.repository.AnnouncementRepository;


import org.springframework.transaction.annotation.Transactional;
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
        return announcementMapper.aDtos(announcementRepository.findAllByOrderByFechaCreacionDesc());
    }

    // TODO: pendiente de HU futura (editar anuncio).
    @Transactional
    public AnnouncementResponse actualizarAnuncio(UUID id, AnnouncementRequest request) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    // TODO: pendiente de HU futura (eliminar anuncio).
    @Transactional
    public void eliminarAnuncio(UUID id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

}
