package com.orquidea.api.service;

import com.orquidea.api.dto.AdministratorDto;
import com.orquidea.api.dto.AdministratorUpdateRequest;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.exception.ResourceNotFoundException;
import com.orquidea.api.mapper.AdministratorMapper;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministratorService {

    private static final String MENSAJE_NO_EXISTE = "El administrador solicitado no existe.";
    private static final String MENSAJE_CORREO_REGISTRADO = "Este correo ya está registrado.";
    private static final String MENSAJE_UNICO_SUPERADMIN = "No puedes revocar el único superadministrador de la plataforma.";

    private final UserRepository userRepository;
    private final AdministratorMapper administratorMapper;
    private final EmailNotificationService notificacionServicio;

    /** HU-5, escenario 1. Solo cuentas con rol ADMINISTRADOR; inhabilitar es poner habilitado en false. */
    @Transactional
    public AdministratorDto actualizar(UUID id, AdministratorUpdateRequest solicitud) {
        User administrador = userRepository.findByIdAndRol(id, Role.ADMINISTRADOR)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
        String correo = AuthService.normalizarCorreo(solicitud.getCorreo());
        if (userRepository.existsByCorreoAndIdNot(correo, id)) {
            throw new DuplicateResourceException(MENSAJE_CORREO_REGISTRADO);
        }
        administratorMapper.actualizar(solicitud, administrador);
        administrador.setCorreo(correo);
        return administratorMapper.aDto(userRepository.saveAndFlush(administrador));
    }

    /**
     * HU-5, escenarios 2 y 3. Degrada la cuenta a USUARIO_REGISTRADO y avisa por correo una vez confirmada
     * la transacción. El único superadministrador activo no puede revocarse.
     */
    @Transactional
    public AdministratorDto revocarAcceso(UUID id) {
        User usuario = userRepository.findByIdAndRolIn(id, List.of(Role.ADMINISTRADOR, Role.SUPERADMINISTRADOR))
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_NO_EXISTE));
        if (usuario.getRol() == Role.SUPERADMINISTRADOR
                && userRepository.countByRolAndHabilitadoTrue(Role.SUPERADMINISTRADOR) <= 1) {
            throw new OperationNotAllowedException(MENSAJE_UNICO_SUPERADMIN);
        }
        usuario.setRol(Role.USUARIO_REGISTRADO);
        User revocado = userRepository.saveAndFlush(usuario);
        alConfirmar(() -> notificacionServicio.notificarRevocacionAcceso(revocado.getCorreo(), revocado.getNombre()));
        return administratorMapper.aDto(revocado);
    }

    private static void alConfirmar(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}