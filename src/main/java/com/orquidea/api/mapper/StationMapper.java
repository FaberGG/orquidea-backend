package com.orquidea.api.mapper;

import com.orquidea.api.dto.StationDto;
import com.orquidea.api.dto.StationRequest;
import com.orquidea.api.dto.StationTaxonDto;
import com.orquidea.api.model.Station;
import com.orquidea.api.model.Taxon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotoClave", ignore = true)
    @Mapping(target = "fichas", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Station aEntidad(StationRequest solicitud);

    /** Copia los datos editables sobre la estación existente; la foto y las fichas las maneja el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotoClave", ignore = true)
    @Mapping(target = "fichas", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void actualizar(StationRequest solicitud, @MappingTarget Station estacion);

    /** Las URLs dependen del almacenamiento, por eso llegan aparte. */
    @Mapping(target = "urlFoto", source = "urlFoto")
    @Mapping(target = "fichas", source = "fichasDto")
    StationDto aDto(Station estacion, String urlFoto, List<StationTaxonDto> fichasDto);

    @Mapping(target = "urlFoto", source = "urlFoto")
    StationTaxonDto aFichaDto(Taxon ficha, String urlFoto);
}
