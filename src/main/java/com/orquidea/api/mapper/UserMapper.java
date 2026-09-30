package com.orquidea.api.mapper;

import com.orquidea.api.dto.RegisterResponse;
import com.orquidea.api.dto.AuthenticatedUserDto;
import com.orquidea.api.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    AuthenticatedUserDto aUsuarioAutenticadoDto(User usuario);
    
    RegisterResponse aRespuestaRegistro(User usuario);
}
