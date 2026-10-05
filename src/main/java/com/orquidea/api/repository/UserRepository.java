package com.orquidea.api.repository;

import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCorreo(String correo);
    
    Optional<User> findByIdAndRol(UUID id, Role rol);

    Optional<User> findByIdAndRolIn(UUID id, Collection<Role> roles);

    List<User> findByRolIn(Collection<Role> roles, Sort orden);
    
    boolean existsByRol(Role rol);

    boolean existsByCorreo(String correo);
    
    boolean existsByCorreoAndIdNot(String correo, UUID id);

    long countByRolAndHabilitadoTrue(Role rol);
}
