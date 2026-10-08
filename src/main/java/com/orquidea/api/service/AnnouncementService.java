package com.orquidea.api.service;

import java.util.List;

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

}
