package com.orquidea.api.service;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import com.orquidea.api.service.PhotoReprocessingService.ReprocessingSummary;
import com.orquidea.api.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.orquidea.api.storage.TestImages.APP1;
import static com.orquidea.api.storage.TestImages.conSegmento;
import static com.orquidea.api.storage.TestImages.exif;
import static com.orquidea.api.storage.TestImages.fotografia;
import static com.orquidea.api.storage.TestImages.jpeg;
import static com.orquidea.api.storage.TestImages.leer;
import static com.orquidea.api.storage.TestImages.png;
import static com.orquidea.api.storage.TestImages.segmentos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reprocesamiento de las fotos subidas antes de la optimización. Esas fotos se simulan subiendo el archivo tal
 * cual al almacenamiento y apuntando a él desde la base de datos, sin miniatura.
 */
class PhotoReprocessingServiceTest extends IntegrationTestBase {

    private static final String COMPONENTE = "OTROS";

    @Autowired
    private PhotoReprocessingService photoReprocessingService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JdbcTemplate jdbc;

    /** Filas que no se borran solas y podrían afectar a otras pruebas. */
    private final List<Runnable> limpiezas = new ArrayList<>();

    @AfterEach
    void limpiar() {
        limpiezas.forEach(Runnable::run);
    }

    @Test
    @DisplayName("Optimiza las fotos antiguas, les crea miniatura y avisa a la app sin conexión; repetirlo no cambia nada")
    void reprocesaLasFotosAntiguas() throws Exception {
        // Ficha con una foto antigua: PNG sin transparencia, que pasa a JPEG con clave nueva
        String idFicha = crearFicha();
        String claveFicha = subirTalCual("fichas/", png(fotografia(2400, 1800)), "png");
        UUID idFotoFicha = UUID.randomUUID();
        jdbc.update("INSERT INTO fotos_ficha (id, ficha_id, foto_clave, posicion) VALUES (?, ?, ?, 5)",
                idFotoFicha, UUID.fromString(idFicha), claveFicha);
        Timestamp fechaFicha = Timestamp.valueOf("2020-01-01 00:00:00");
        jdbc.update("UPDATE fichas_taxonomicas SET fecha_actualizacion = ? WHERE id = ?", fechaFicha,
                UUID.fromString(idFicha));

        // Estación con una foto de celular: grande, con EXIF y GPS
        String claveEstacion = subirTalCual("estaciones/",
                conSegmento(jpeg(fotografia(3200, 2400), 0.98f), APP1, exif(1, "TelefonoDePrueba")), "jpg");
        UUID idEstacion = UUID.randomUUID();
        jdbc.update("INSERT INTO estaciones (id, nombre, descripcion, foto_clave, latitud, longitud) "
                        + "VALUES (?, 'Estación antigua', 'Para reprocesar', ?, ?, ?)",
                idEstacion, claveEstacion, new BigDecimal("2.4598"), new BigDecimal("-76.6115"));
        limpiezas.add(() -> jdbc.update("DELETE FROM estaciones WHERE id = ?", idEstacion));

        // Componente con una foto que ya estaba optimizada: conserva su clave y solo gana la miniatura
        byte[] optimizada = jpeg(fotografia(800, 600), 0.75f);
        String claveComponente = subirTalCual("componentes/", optimizada, "jpg");
        UUID idFotoComponente = UUID.randomUUID();
        jdbc.update("INSERT INTO fotos_componente (id, componente_tipo, foto_clave) VALUES (?, ?, ?)",
                idFotoComponente, COMPONENTE, claveComponente);
        limpiezas.add(() -> jdbc.update("DELETE FROM fotos_componente WHERE id = ?", idFotoComponente));

        // Reporte cuya foto ya no está en el almacenamiento: falla sin detener a las demás
        String idReporte = crearReporte();
        String claveFaltante = "reportes/" + UUID.randomUUID() + ".jpg";
        jdbc.update("UPDATE reportes_avistamiento SET foto_clave = ? WHERE id = ?", claveFaltante,
                UUID.fromString(idReporte));

        // Simulación: calcula el ahorro sin tocar nada
        ReprocessingSummary simulacion = photoReprocessingService.reprocesar(false);
        assertThat(simulacion.procesadas()).isGreaterThanOrEqualTo(3);
        assertThat(simulacion.fallidas()).isGreaterThanOrEqualTo(1);
        assertThat(simulacion.bytesDespues()).isLessThan(simulacion.bytesAntes());
        assertThat(fila("fotos_ficha", idFotoFicha)).containsEntry("foto_clave", claveFicha)
                .containsEntry("miniatura_clave", null);
        assertThat(fila("estaciones", idEstacion)).containsEntry("miniatura_clave", null);
        assertThat(fila("fotos_componente", idFotoComponente)).containsEntry("miniatura_clave", null);
        assertThat(storageService.descargar(claveEstacion)).isNotEmpty();

        // Aplicar
        photoReprocessingService.reprocesar(true);

        Map<String, Object> fotoFicha = fila("fotos_ficha", idFotoFicha);
        String nuevaClaveFicha = (String) fotoFicha.get("foto_clave");
        assertThat(nuevaClaveFicha).startsWith("fichas/").endsWith(".jpg").isNotEqualTo(claveFicha);
        assertThat((String) fotoFicha.get("miniatura_clave")).startsWith("fichas/miniaturas/");
        assertThat(leer(storageService.descargar(nuevaClaveFicha)).getWidth()).isEqualTo(1600);
        assertThat(leer(storageService.descargar((String) fotoFicha.get("miniatura_clave"))).getWidth())
                .isEqualTo(480);
        assertNoExiste(claveFicha);
        assertThat(jdbc.queryForObject("SELECT fecha_actualizacion FROM fichas_taxonomicas WHERE id = ?",
                Timestamp.class, UUID.fromString(idFicha))).isAfter(fechaFicha);

        Map<String, Object> estacion = fila("estaciones", idEstacion);
        String nuevaClaveEstacion = (String) estacion.get("foto_clave");
        assertThat(nuevaClaveEstacion).isNotEqualTo(claveEstacion);
        assertThat(estacion.get("miniatura_clave")).isNotNull();
        byte[] fotoEstacion = storageService.descargar(nuevaClaveEstacion);
        assertThat(segmentos(fotoEstacion)).noneMatch(segmento -> segmento.marcador() == APP1);
        BufferedImage imagenEstacion = leer(fotoEstacion);
        assertThat(imagenEstacion.getWidth()).isEqualTo(1600);
        assertNoExiste(claveEstacion);

        Map<String, Object> fotoComponente = fila("fotos_componente", idFotoComponente);
        assertThat(fotoComponente).containsEntry("foto_clave", claveComponente)
                .containsEntry("miniatura_clave", claveComponente.replace("componentes/", "componentes/miniaturas/"));
        assertThat(storageService.descargar(claveComponente)).isEqualTo(optimizada);

        assertThat(fila("reportes_avistamiento", UUID.fromString(idReporte)))
                .containsEntry("foto_clave", claveFaltante)
                .containsEntry("miniatura_clave", null);

        // Repetirlo no vuelve a procesar las que ya tienen miniatura
        photoReprocessingService.reprocesar(true);
        assertThat(fila("fotos_ficha", idFotoFicha)).containsEntry("foto_clave", nuevaClaveFicha);
        assertThat(fila("estaciones", idEstacion)).containsEntry("foto_clave", nuevaClaveEstacion);
    }

    private String subirTalCual(String carpeta, byte[] contenido, String extension) {
        String clave = carpeta + UUID.randomUUID() + "." + extension;
        storageService.subir(clave, contenido, extension.equals("png") ? "image/png" : "image/jpeg");
        return clave;
    }

    private Map<String, Object> fila(String tabla, UUID id) {
        return jdbc.queryForMap("SELECT foto_clave, miniatura_clave FROM " + tabla + " WHERE id = ?", id);
    }

    private void assertNoExiste(String clave) {
        assertThatThrownBy(() -> storageService.descargar(clave)).isInstanceOf(NoSuchKeyException.class);
    }

    private String crearFicha() throws Exception {
        var solicitud = multipart("/api/fichas-taxonomicas")
                .file(new MockMultipartFile("fotos", "azulejo.png", "image/png", png(fotografia(40, 30))));
        Map.of("categoria", "AVE", "order", "Passeriformes", "family", "Thraupidae", "genus", "Thraupis",
                        "scientificName", "Thraupis reprocesada " + UUID.randomUUID(), "vernacularName", "Azulejo",
                        "alimentacion", "Frutos", "rolEnHumedal", "Dispersor de semillas", "estadoConservacion", "LC")
                .forEach(solicitud::param);
        solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeUsuarioNuevo(Role.ADMINISTRADOR));
        String respuesta = mockMvc.perform(solicitud).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private String crearReporte() throws Exception {
        var solicitud = multipart("/api/reportes-avistamiento").param("occurrenceRemarks", "Reporte antiguo");
        solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO));
        String respuesta = mockMvc.perform(solicitud).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }
}
