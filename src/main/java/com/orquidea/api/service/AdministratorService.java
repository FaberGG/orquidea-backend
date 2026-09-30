package com.orquidea.api.service;

import com.orquidea.api.dto.AdministratorDto;
import com.orquidea.api.dto.AdministratorUpdateRequest;
import com.orquidea.api.exception.OperationNotAllowedException;
import com.orquidea.api.exception.RecursoDuplicadoExcepcion;
import com.orquidea.api.exception.RecursoNoEncontradoExcepcion;
import com.orquidea.api.mapper.AdministratorMapper;
import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import com.orquidea.api.repository.UsuarioRepositorio;
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

    private final UsuarioRepositorio usuarioRepositorio;
    private final AdministratorMapper administratorMapper;
    private final EmailNotificationService notificacionServicio;

    /** HU-5, escenario 1. Solo cuentas con rol ADMINISTRADOR; inhabilitar es poner habilitado en false. */
    @Transactional
    public AdministratorDto actualizar(UUID id, AdministratorUpdateRequest solicitud) {
        Usuario administrador = usuarioRepositorio.findByIdAndRol(id, Rol.ADMINISTRADOR)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(MENSAJE_NO_EXISTE));
        String correo = AutenticacionServicio.normalizarCorreo(solicitud.getCorreo());
        if (usuarioRepositorio.existsByCorreoAndIdNot(correo, id)) {
            throw new RecursoDuplicadoExcepcion(MENSAJE_CORREO_REGISTRADO);
        }
        administratorMapper.actualizar(solicitud, administrador);
        administrador.setCorreo(correo);
        return administratorMapper.aDto(usuarioRepositorio.saveAndFlush(administrador));
    }

    /**
     * HU-5, escenarios 2 y 3. Degrada la cuenta a USUARIO_REGISTRADO y avisa por correo una vez confirmada
     * la transacción. El único superadministrador activo no puede revocarse.
     */
    @Transactional
    public AdministratorDto revocarAcceso(UUID id) {
        Usuario usuario = usuarioRepositorio.findByIdAndRolIn(id, List.of(Rol.ADMINISTRADOR, Rol.SUPERADMINISTRADOR))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(MENSAJE_NO_EXISTE));
        if (usuario.getRol() == Rol.SUPERADMINISTRADOR
                && usuarioRepositorio.countByRolAndHabilitadoTrue(Rol.SUPERADMINISTRADOR) <= 1) {
            throw new OperationNotAllowedException(MENSAJE_UNICO_SUPERADMIN);
        }
        usuario.setRol(Rol.USUARIO_REGISTRADO);
        Usuario revocado = usuarioRepositorio.saveAndFlush(usuario);
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