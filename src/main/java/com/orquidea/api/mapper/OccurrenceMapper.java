package com.orquidea.api.mapper;

import com.orquidea.api.dto.OccurrenceDto;
import com.orquidea.api.model.Occurrence;
import com.orquidea.api.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OccurrenceMapper {

    /** Las URLs de la foto dependen del almacenamiento, por eso llegan aparte. */
    @Mapping(target = "urlFoto", source = "urlFoto")
    @Mapping(target = "urlMiniatura", source = "urlMiniatura")
    @Mapping(target = "recordedBy", source = "reporte.recordedBy")
    OccurrenceDto aDto(Occurrence reporte, String urlFoto, String urlMiniatura);

    /** Del autor solo se expone el nombre, nunca el correo. */
    default String nombreDe(User autor) {
        return autor.getNombre() + " " + autor.getApellido();
    }
}
