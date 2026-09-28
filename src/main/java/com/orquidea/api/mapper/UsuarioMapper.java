package com.orquidea.api.mapper;

import com.orquidea.api.dto.UsuarioAutenticadoDto;
import com.orquidea.api.model.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioAutenticadoDto aUsuarioAutenticadoDto(Usuario usuario);
}
