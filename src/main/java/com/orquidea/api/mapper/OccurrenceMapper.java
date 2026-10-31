package com.orquidea.api.mapper;

import com.orquidea.api.dto.OccurrenceDto;
import com.orquidea.api.model.Occurrence;
import com.orquidea.api.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OccurrenceMapper {

    /** La URL de la foto depende del almacenamiento, por eso llega aparte. */
    @Mapping(target = "urlFoto", source = "urlFoto")
    @Mapping(target = "recordedBy", source = "reporte.recordedBy")
    OccurrenceDto aDto(Occurrence reporte, String urlFoto);

    /** Del autor solo se expone el nombre, nunca el correo. */
    default String nombreDe(User autor) {
        return autor.getNombre() + " " + autor.getApellido();
    }
}
