package com.orquidea.api.mapper;

import com.orquidea.api.dto.TaxonDto;
import com.orquidea.api.dto.TaxonPhotoDto;
import com.orquidea.api.dto.TaxonRequest;
import com.orquidea.api.model.Taxon;
import com.orquidea.api.model.TaxonPhoto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaxonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotos", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Taxon aEntidad(TaxonRequest solicitud);

    /** Copia los datos editables sobre la ficha existente; las fotos las maneja el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fotos", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void actualizar(TaxonRequest solicitud, @MappingTarget Taxon ficha);

    /** Las URLs dependen del almacenamiento, por eso llegan aparte; urlFoto es la de la foto principal. */
    @Mapping(target = "fotos", source = "fotosDto")
    @Mapping(target = "urlFoto", expression = "java(fotosDto.isEmpty() ? null : fotosDto.get(0).getUrl())")
    TaxonDto aDto(Taxon ficha, List<TaxonPhotoDto> fotosDto);

    @Mapping(target = "url", source = "url")
    TaxonPhotoDto aFotoDto(TaxonPhoto foto, String url);
}
