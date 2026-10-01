package com.orquidea.api.controllers;

import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.PasswordResetCode;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.PasswordResetCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recuperación de contraseña con código de 6 dígitos (endpoints de AuthController). */
class PasswordResetControllerTest extends IntegrationTestBase {

    private static final String RUTA_RECUPERAR = "/api/autenticacion/recuperar-contrasena";
    private static final String RUTA_RESTABLECER = "/api/autenticacion/restablecer-contrasena";
    private static final String MENSAJE_CODIGO_INVALIDO = "El código es incorrecto o ya venció. Solicita uno nuevo.";
    private static final String CONTRASENA_ORIGINAL = "Clave-Original-1";
    private static final String CONTRASENA_NUEVA = "Clave-Nueva-2";
    /** El envío es asíncrono: se espera a que el mock reciba la llamada. */
    private static final int ESPERA_CORREO_MS = 2000;

    @Autowired
    private PasswordResetCodeRepository codigoRepositorio;

    private ResultActions solicitarCodigo(String correo) throws Exception {
        return mockMvc.perform(post(RUTA_RECUPERAR)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\"}".formatted(correo)));
    }

    private ResultActions restablecer(String correo, String codigo, String contrasena) throws Exception {
        return mockMvc.perform(post(RUTA_RESTABLECER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"codigo\":\"%s\",\"contrasena\":\"%s\"}"
                        .formatted(correo, codigo, contrasena)));
    }

    /** Pide un código y devuelve el que se envió por correo. */
    private String codigoEnviadoA(String correo) throws Exception {
        solicitarCodigo(correo).andExpect(status().isNoContent());
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(notificacionServicio, timeout(ESPERA_CORREO_MS))
                .enviarCodigoRecuperacion(eq(correo), anyString(), codigo.capture(), any(Duration.class));
        return codigo.getValue();
    }

    private String codigoDistintoDe(String codigo) {
        return codigo.equals("000000") ? "111111" : "000000";
    }

    private void modificarCodigoDe(String correo, Consumer<PasswordResetCode> cambio) {
        UUID idUsuario = userRepository.findByCorreo(correo).orElseThrow().getId();
        PasswordResetCode registro = codigoRepositorio.findAll().stream()
                .filter(c -> c.getUsuario().getId().equals(idUsuario))
                .findFirst()
                .orElseThrow();
        cambio.accept(registro);
        codigoRepositorio.save(registro);
    }

    private void esperarCodigoInvalido(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_CODIGO_INVALIDO));
    }

    // Solicitud del código

    @Test
    @DisplayName("Se envía un código de 6 dígitos al correo registrado")
    void enviaCodigoDeSeisDigitos() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);

        assertThat(codigoEnviadoA(correo)).matches("\\d{6}");
    }

    @Test
    @DisplayName("El código no se guarda en texto plano")
    void codigoGuardadoComoHash() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);

        modificarCodigoDe(correo, registro -> {
            assertThat(registro.getCodigoHash()).doesNotContain(codigo);
            assertThat(codificadorContrasenas.matches(codigo, registro.getCodigoHash())).isTrue();
        });
    }

    @Test
    @DisplayName("Un correo no registrado recibe la misma respuesta y no se envía nada")
    void correoNoRegistrado() throws Exception {
        solicitarCodigo("nadie-" + UUID.randomUUID() + "@prueba.local").andExpect(status().isNoContent());

        verify(notificacionServicio, after(300).never())
                .enviarCodigoRecuperacion(anyString(), anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Una cuenta deshabilitada no recibe código")
    void cuentaDeshabilitada() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, false);

        solicitarCodigo(correo).andExpect(status().isNoContent());

        verify(notificacionServicio, after(300).never())
                .enviarCodigoRecuperacion(anyString(), anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("El correo no distingue mayúsculas")
    void correoEnMayusculas() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);

        solicitarCodigo(correo.toUpperCase()).andExpect(status().isNoContent());

        verify(notificacionServicio, timeout(ESPERA_CORREO_MS))
                .enviarCodigoRecuperacion(eq(correo), anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Pedir otro código antes de la espera de reenvío no envía un segundo correo")
    void esperaDeReenvio() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);

        solicitarCodigo(correo).andExpect(status().isNoContent());

        verify(notificacionServicio, after(300).times(1))
                .enviarCodigoRecuperacion(eq(correo), anyString(), anyString(), any(Duration.class));
        restablecer(correo, codigo, CONTRASENA_NUEVA).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Un código nuevo reemplaza al anterior")
    void codigoNuevoReemplazaAlAnterior() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String primero = codigoEnviadoA(correo);
        modificarCodigoDe(correo, registro -> registro.setFechaEmision(Instant.now().minus(Duration.ofMinutes(2))));

        solicitarCodigo(correo).andExpect(status().isNoContent());
        ArgumentCaptor<String> codigos = ArgumentCaptor.forClass(String.class);
        verify(notificacionServicio, timeout(ESPERA_CORREO_MS).times(2))
                .enviarCodigoRecuperacion(eq(correo), anyString(), codigos.capture(), any(Duration.class));
        String segundo = codigos.getAllValues().get(1);

        if (!primero.equals(segundo)) {
            esperarCodigoInvalido(restablecer(correo, primero, CONTRASENA_NUEVA));
        }
        restablecer(correo, segundo, CONTRASENA_NUEVA).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("El correo es obligatorio y debe ser válido")
    void correoInvalido() throws Exception {
        solicitarCodigo("").andExpect(status().isBadRequest());
        solicitarCodigo("no-es-un-correo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Ingresa un correo electrónico válido."));
    }

    // Restablecimiento

    @Test
    @DisplayName("Con el código correcto se cambia la contraseña y se puede iniciar sesión con la nueva")
    void restablecimientoExitoso() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);

        restablecer(correo, codigo, CONTRASENA_NUEVA).andExpect(status().isNoContent());

        iniciarSesion(correo, CONTRASENA_NUEVA).andExpect(status().isOk());
        iniciarSesion(correo, CONTRASENA_ORIGINAL).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El código es de un solo uso")
    void codigoDeUnSoloUso() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);
        restablecer(correo, codigo, CONTRASENA_NUEVA).andExpect(status().isNoContent());

        esperarCodigoInvalido(restablecer(correo, codigo, "Otra-Clave-3"));
        iniciarSesion(correo, CONTRASENA_NUEVA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un código incorrecto no cambia la contraseña")
    void codigoIncorrecto() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);

        esperarCodigoInvalido(restablecer(correo, codigoDistintoDe(codigo), CONTRASENA_NUEVA));

        iniciarSesion(correo, CONTRASENA_ORIGINAL).andExpect(status().isOk());
        restablecer(correo, codigo, CONTRASENA_NUEVA).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos el código deja de servir")
    void limiteDeIntentos() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);
        String incorrecto = codigoDistintoDe(codigo);

        for (int intento = 0; intento < 5; intento++) {
            esperarCodigoInvalido(restablecer(correo, incorrecto, CONTRASENA_NUEVA));
        }

        esperarCodigoInvalido(restablecer(correo, codigo, CONTRASENA_NUEVA));
        iniciarSesion(correo, CONTRASENA_ORIGINAL).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un código vencido no sirve")
    void codigoVencido() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);
        modificarCodigoDe(correo, registro -> registro.setFechaExpiracion(Instant.now().minusSeconds(1)));

        esperarCodigoInvalido(restablecer(correo, codigo, CONTRASENA_NUEVA));
        iniciarSesion(correo, CONTRASENA_ORIGINAL).andExpect(status().isOk());
    }

    @Test
    @DisplayName("El código de un usuario no sirve para otro")
    void codigoDeOtroUsuario() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String otroCorreo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);

        esperarCodigoInvalido(restablecer(otroCorreo, codigo, CONTRASENA_NUEVA));
    }

    @Test
    @DisplayName("Un correo sin código recibe el mismo mensaje")
    void correoSinCodigo() throws Exception {
        esperarCodigoInvalido(restablecer("nadie-" + UUID.randomUUID() + "@prueba.local", "123456", CONTRASENA_NUEVA));
    }

    @Test
    @DisplayName("Si la cuenta se deshabilita, su código deja de servir")
    void cuentaDeshabilitadaTrasPedirCodigo() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);
        String codigo = codigoEnviadoA(correo);
        User usuario = userRepository.findByCorreo(correo).orElseThrow();
        usuario.setHabilitado(false);
        userRepository.save(usuario);

        esperarCodigoInvalido(restablecer(correo, codigo, CONTRASENA_NUEVA));
    }

    @Test
    @DisplayName("El código debe tener 6 dígitos y la contraseña es obligatoria")
    void validacionDeCampos() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, CONTRASENA_ORIGINAL, true);

        restablecer(correo, "12ab56", CONTRASENA_NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El código debe tener 6 dígitos."));
        restablecer(correo, "123456", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La nueva contraseña es obligatoria."));
    }
}
