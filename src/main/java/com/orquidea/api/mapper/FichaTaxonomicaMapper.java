package com.orquidea.api.mapper;

import com.orquidea.api.dto.FichaTaxonomicaDto;
import com.orquidea.api.dto.SolicitudFichaTaxonomica;
import com.orquidea.api.model.FichaTaxonomica;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FichaTaxonomicaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotoClave", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    FichaTaxonomica aEntidad(SolicitudFichaTaxonomica solicitud);

    /** Copia los datos editables sobre la ficha existente; la foto la maneja el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotoClave", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void actualizar(SolicitudFichaTaxonomica solicitud, @MappingTarget FichaTaxonomica ficha);

    /** La URL de la foto depende del almacenamiento, por eso llega aparte. */
    @Mapping(target = "urlFoto", source = "urlFoto")
    FichaTaxonomicaDto aDto(FichaTaxonomica ficha, String urlFoto);
}
