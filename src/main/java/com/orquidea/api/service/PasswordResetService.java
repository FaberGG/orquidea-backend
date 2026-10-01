package com.orquidea.api.service;

import com.orquidea.api.config.PasswordResetProperties;
import com.orquidea.api.dto.PasswordRecoveryRequest;
import com.orquidea.api.dto.PasswordResetRequest;
import com.orquidea.api.exception.InvalidRequestException;
import com.orquidea.api.model.PasswordResetCode;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.PasswordResetCodeRepository;
import com.orquidea.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * Recuperación de contraseña con un código de 6 dígitos enviado por correo.
 * Ninguna respuesta revela si un correo está registrado: la solicitud siempre responde igual y
 * cualquier fallo al restablecer da el mismo mensaje. Por eso también se calcula un hash BCrypt
 * aunque el usuario no exista, para que el tiempo de respuesta tampoco lo delate.
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final String MENSAJE_CODIGO_INVALIDO = "El código es incorrecto o ya venció. Solicita uno nuevo.";
    private static final int TOTAL_CODIGOS = 1_000_000;

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codigoRepositorio;
    private final PasswordEncoder codificadorContrasenas;
    private final EmailNotificationService notificacionServicio;
    private final PasswordResetProperties propiedades;
    private final SecureRandom aleatorio = new SecureRandom();
    /** Hash contra el que se compara cuando no hay código, para igualar el tiempo de respuesta. */
    private final String hashFicticio;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetCodeRepository codigoRepositorio,
                                PasswordEncoder codificadorContrasenas,
                                EmailNotificationService notificacionServicio,
                                PasswordResetProperties propiedades) {
        this.userRepository = userRepository;
        this.codigoRepositorio = codigoRepositorio;
        this.codificadorContrasenas = codificadorContrasenas;
        this.notificacionServicio = notificacionServicio;
        this.propiedades = propiedades;
        this.hashFicticio = codificadorContrasenas.encode("000000");
    }

    /**
     * Emite un código y lo envía por correo, solo si la cuenta existe y está habilitada.
     * Si el usuario ya tiene un código emitido hace menos de la espera de reenvío, no hace nada.
     */
    @Transactional
    public void solicitarCodigo(PasswordRecoveryRequest solicitud) {
        String codigo = "%06d".formatted(aleatorio.nextInt(TOTAL_CODIGOS));
        String codigoHash = codificadorContrasenas.encode(codigo);

        Optional<User> cuenta = userRepository.findByCorreo(AuthService.normalizarCorreo(solicitud.getCorreo()))
                .filter(User::isHabilitado);
        if (cuenta.isEmpty()) {
            return;
        }
        User usuario = cuenta.get();
        Instant ahora = Instant.now();
        PasswordResetCode registro = codigoRepositorio.findByUsuario(usuario)
                .orElseGet(() -> PasswordResetCode.builder().usuario(usuario).build());
        if (registro.getFechaEmision() != null
                && registro.getFechaEmision().plus(propiedades.esperaReenvio()).isAfter(ahora)) {
            return;
        }

        registro.setCodigoHash(codigoHash);
        registro.setIntentos(0);
        registro.setFechaEmision(ahora);
        registro.setFechaExpiracion(ahora.plus(propiedades.vigencia()));
        codigoRepositorio.save(registro);

        alConfirmar(() -> notificacionServicio.enviarCodigoRecuperacion(
                usuario.getCorreo(), usuario.getNombre(), codigo, propiedades.vigencia()));
    }

    /**
     * Cambia la contraseña si el código es correcto y sigue vigente; el código se elimina al usarse.
     * Los intentos fallidos se guardan aunque se lance la excepción (noRollbackFor), y al llegar
     * al máximo el código se elimina para frenar los intentos por fuerza bruta.
     */
    @Transactional(noRollbackFor = InvalidRequestException.class)
    public void restablecerContrasena(PasswordResetRequest solicitud) {
        Optional<PasswordResetCode> encontrado = userRepository
                .findByCorreo(AuthService.normalizarCorreo(solicitud.getCorreo()))
                .filter(User::isHabilitado)
                .flatMap(codigoRepositorio::findByUsuario);
        boolean coincide = codificadorContrasenas.matches(solicitud.getCodigo(),
                encontrado.map(PasswordResetCode::getCodigoHash).orElse(hashFicticio));
        PasswordResetCode registro = encontrado.orElseThrow(PasswordResetService::codigoInvalido);

        if (registro.getFechaExpiracion().isBefore(Instant.now())) {
            codigoRepositorio.delete(registro);
            throw codigoInvalido();
        }
        if (!coincide) {
            registro.setIntentos(registro.getIntentos() + 1);
            if (registro.getIntentos() >= propiedades.intentosMaximos()) {
                codigoRepositorio.delete(registro);
            }
            throw codigoInvalido();
        }

        registro.getUsuario().setContrasenaHash(codificadorContrasenas.encode(solicitud.getContrasena()));
        codigoRepositorio.delete(registro);
    }

    private static InvalidRequestException codigoInvalido() {
        return new InvalidRequestException(MENSAJE_CODIGO_INVALIDO);
    }

    /** El correo sale solo si el código quedó guardado. */
    private static void alConfirmar(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
