package com.orquidea.api.repository;

import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByRol(Rol rol);

    boolean existsByCorreo(String correo);
}
