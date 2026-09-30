package com.orquidea.api.repository;

import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);
    
    Optional<Usuario> findByIdAndRol(UUID id, Rol rol);

    Optional<Usuario> findByIdAndRolIn(UUID id, Collection<Rol> roles);
    
    boolean existsByRol(Rol rol);

    boolean existsByCorreo(String correo);
    
    boolean existsByCorreoAndIdNot(String correo, UUID id);

    long countByRolAndHabilitadoTrue(Rol rol);
}
