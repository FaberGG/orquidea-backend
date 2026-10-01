package com.orquidea.api.repository;

import com.orquidea.api.model.PasswordResetCode;
import com.orquidea.api.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    /**
     * Bloquea la fila para que peticiones simultáneas no superen el límite de intentos
     * ni emitan dos códigos a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetCode> findByUsuario(User usuario);
}
