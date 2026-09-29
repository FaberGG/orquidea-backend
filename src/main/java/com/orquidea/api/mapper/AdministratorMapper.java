package com.orquidea.api.mapper;

import com.orquidea.api.dto.AdministratorDto;
import com.orquidea.api.dto.AdministratorUpdateRequest;
import com.orquidea.api.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AdministratorMapper {

    AdministratorDto aDto(Usuario usuario);

    /** El correo lo asigna el servicio (normalizado); el rol y la contraseña no se editan aquí. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "correo", ignore = true)
    @Mapping(target = "contrasenaHash", ignore = true)
    @Mapping(target = "rol", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void actualizar(AdministratorUpdateRequest solicitud, @MappingTarget Usuario usuario);
}