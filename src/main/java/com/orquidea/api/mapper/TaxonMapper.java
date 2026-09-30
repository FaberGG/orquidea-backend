package com.orquidea.api.mapper;

import com.orquidea.api.dto.TaxonDto;
import com.orquidea.api.dto.TaxonRequest;
import com.orquidea.api.model.Taxon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaxonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotoClave", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Taxon aEntidad(TaxonRequest solicitud);

    /** Copia los datos editables sobre la ficha existente; la foto la maneja el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotoClave", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void actualizar(TaxonRequest solicitud, @MappingTarget Taxon ficha);

    /** La URL de la foto depende del almacenamiento, por eso llega aparte. */
    @Mapping(target = "urlFoto", source = "urlFoto")
    TaxonDto aDto(Taxon ficha, String urlFoto);
}
