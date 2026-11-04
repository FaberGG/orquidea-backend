package com.orquidea.api.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.orquidea.api.dto.AnnouncementRequest;
import com.orquidea.api.dto.AnnouncementResponse;
import com.orquidea.api.dto.AnnouncementUpdateRequest;
import com.orquidea.api.model.Announcement;

@Mapper(componentModel = "spring")
public interface AnnouncementMapper {

    /** El id y la fecha los asigna el sistema, nunca vienen del cliente. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Announcement aEntidad(AnnouncementRequest solicitud);

    @Mapping(target = "expirado", expression = "java(anuncio.getFechaExpiracion() != null && !anuncio.getFechaExpiracion().isAfter(java.time.Instant.now()))")
    AnnouncementResponse aDto(Announcement anuncio);

    List<AnnouncementResponse> aDtos(List<Announcement> anuncios);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void actualizarEntidad(AnnouncementUpdateRequest request, @MappingTarget Announcement anuncio);
}