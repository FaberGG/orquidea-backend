package com.orquidea.api.mapper;

import com.orquidea.api.dto.ComponentDto;
import com.orquidea.api.dto.ComponentPhotoDto;
import com.orquidea.api.dto.ComponentUpdateRequest;
import com.orquidea.api.model.WetlandComponent;
import com.orquidea.api.model.WetlandComponentPhoto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComponentMapper {

    /** Las URLs dependen del almacenamiento y "tieneContenido" de la regla del servicio, por eso llegan aparte. */
    @Mapping(target = "fotos", source = "fotosDto")
    @Mapping(target = "tieneContenido", source = "tieneContenido")
    ComponentDto aDto(WetlandComponent componente, List<ComponentPhotoDto> fotosDto, boolean tieneContenido);

    @Mapping(target = "url", source = "url")
    ComponentPhotoDto aFotoDto(WetlandComponentPhoto foto, String url);

    /** Copia el texto editable; el tipo, las fotos y la fecha los maneja el servicio. */
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "fotos", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void actualizar(ComponentUpdateRequest solicitud, @MappingTarget WetlandComponent componente);
}
