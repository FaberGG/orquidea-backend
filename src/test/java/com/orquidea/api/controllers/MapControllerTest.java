package com.orquidea.api.controllers;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-12: el mapa del sendero. El trazado no se edita desde la app, así que las pruebas lo cargan por SQL. */
class MapControllerTest extends IntegrationTestBase {

    private static final String RUTA = "/api/mapa";

    private static final String TRAMO_1 = "{\"type\":\"LineString\",\"coordinates\":[[-76.6120,2.4590],[-76.6110,2.4600]]}";
    private static final String TRAMO_2 = "{\"type\":\"LineString\",\"coordinates\":[[-76.6110,2.4600],[-76.6100,2.4610]]}";

    @Autowired
    private JdbcTemplate jdbc;

    /** El mapa lee todos los tramos y todas las estaciones: cada prueba parte de cero. */
    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM tramos_recorrido");
        jdbc.update("DELETE FROM estaciones");
    }

    @Test
    @DisplayName("HU-12: sin tramos ni estaciones el mapa responde 200 con listas vacías y sin límites")
    void mapaVacio() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.limites").doesNotExist())
                .andExpect(jsonPath("$.tramos", hasSize(0)))
                .andExpect(jsonPath("$.estaciones", hasSize(0)));
    }

    @Test
    @DisplayName("HU-12 escenarios 1 y 4: el visitante, sin sesión, recibe todas las estaciones y el trazado completo en orden")
    void visualizacionExitosa() throws Exception {
        insertarTramo("Tramo norte", 2, TRAMO_2);
        insertarTramo("Tramo sur", 1, TRAMO_1);
        insertarEstacion("Mirador", "2.4595", "-76.6115");
        insertarEstacion("Humedal", "2.4605", "-76.6105");

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramos[*].nombre", contains("Tramo sur", "Tramo norte")))
                .andExpect(jsonPath("$.tramos[*].orden", contains(1, 2)))
                // la geometría llega como objeto GeoJSON, no como texto
                .andExpect(jsonPath("$.tramos[0].geometria.type").value("LineString"))
                .andExpect(jsonPath("$.tramos[0].geometria.coordinates[0][0]").value(-76.612))
                .andExpect(jsonPath("$.tramos[0].geometria.coordinates[1][1]").value(2.46))
                .andExpect(jsonPath("$.estaciones[*].nombre", contains("Humedal", "Mirador")))
                .andExpect(jsonPath("$.estaciones[0].decimalLatitude").value(2.4605))
                .andExpect(jsonPath("$.estaciones[0].decimalLongitude").value(-76.6105))
                .andExpect(jsonPath("$.estaciones[0].urlFoto").isNotEmpty())
                .andExpect(jsonPath("$.estaciones[0].fichas", hasSize(0)));
    }

    @Test
    @DisplayName("HU-12: los límites se calculan con los tramos, como [minLon, minLat, maxLon, maxLat]")
    void limitesDesdeLosTramos() throws Exception {
        insertarTramo("Tramo sur", 1, TRAMO_1);
        insertarTramo("Tramo norte", 2, TRAMO_2);

        mockMvc.perform(get(RUTA))
                .andExpect(jsonPath("$.limites", hasSize(4)))
                .andExpect(jsonPath("$.limites[0]").value(-76.612))
                .andExpect(jsonPath("$.limites[1]").value(2.459))
                .andExpect(jsonPath("$.limites[2]").value(-76.61))
                .andExpect(jsonPath("$.limites[3]").value(2.461));
    }

    @Test
    @DisplayName("HU-12: una estación fuera del trazado también cabe en los límites")
    void limitesIncluyenEstaciones() throws Exception {
        insertarTramo("Tramo sur", 1, TRAMO_1);
        insertarEstacion("Lejana", "2.4700", "-76.6200");

        mockMvc.perform(get(RUTA))
                .andExpect(jsonPath("$.limites[0]").value(-76.62))
                .andExpect(jsonPath("$.limites[1]").value(2.459))
                .andExpect(jsonPath("$.limites[2]").value(-76.611))
                .andExpect(jsonPath("$.limites[3]").value(2.47));
    }

    @Test
    @DisplayName("HU-12: solo con estaciones (sin trazado) los límites salen de las estaciones")
    void limitesSoloConEstaciones() throws Exception {
        insertarEstacion("A", "2.4600", "-76.6100");
        insertarEstacion("B", "2.4500", "-76.6300");

        mockMvc.perform(get(RUTA))
                .andExpect(jsonPath("$.limites[0]").value(-76.63))
                .andExpect(jsonPath("$.limites[1]").value(2.45))
                .andExpect(jsonPath("$.limites[2]").value(-76.61))
                .andExpect(jsonPath("$.limites[3]").value(2.46));
    }

    @Test
    @DisplayName("HU-12 / HE-08: la respuesta trae un ETag igual a la versión y obliga a revalidar")
    void etagIgualALaVersion() throws Exception {
        insertarTramo("Tramo sur", 1, TRAMO_1);

        MvcResult resultado = mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-cache")))
                .andReturn();
        String version = JsonPath.read(resultado.getResponse().getContentAsString(), "$.version");

        assertThat(resultado.getResponse().getHeader(HttpHeaders.ETAG)).isEqualTo("\"" + version + "\"");
    }

    @Test
    @DisplayName("HU-12 / HE-08: con If-None-Match vigente responde 304 sin cuerpo y la versión es estable")
    void respuestaNoModificada() throws Exception {
        insertarTramo("Tramo sur", 1, TRAMO_1);
        insertarEstacion("Mirador", "2.4595", "-76.6115");

        MvcResult primera = mockMvc.perform(get(RUTA)).andExpect(status().isOk()).andReturn();
        String etag = primera.getResponse().getHeader(HttpHeaders.ETAG);
        String segundaVersion = JsonPath.read(mockMvc.perform(get(RUTA)).andReturn().getResponse().getContentAsString(), "$.version");
        assertThat(etag).isEqualTo("\"" + segundaVersion + "\"");

        mockMvc.perform(get(RUTA).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, etag))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("HU-12 / HE-08: la versión cambia al agregar un tramo, editar una estación o borrarla")
    void laVersionCambiaConElContenido() throws Exception {
        insertarTramo("Tramo sur", 1, TRAMO_1);
        UUID estacion = insertarEstacion("Mirador", "2.4595", "-76.6115");
        String inicial = etagActual();

        insertarTramo("Tramo norte", 2, TRAMO_2);
        String conTramoNuevo = etagActual();
        assertThat(conTramoNuevo).isNotEqualTo(inicial);

        jdbc.update("UPDATE estaciones SET nombre = 'Mirador de la laguna' WHERE id = ?", estacion);
        String conEstacionEditada = etagActual();
        assertThat(conEstacionEditada).isNotEqualTo(conTramoNuevo);

        jdbc.update("UPDATE estaciones SET foto_clave = 'estaciones/otra.png' WHERE id = ?", estacion);
        assertThat(etagActual()).isNotEqualTo(conEstacionEditada);

        // con la versión anterior ya no hay 304: el cliente recibe el mapa nuevo
        mockMvc.perform(get(RUTA).header(HttpHeaders.IF_NONE_MATCH, inicial))
                .andExpect(status().isOk());

        jdbc.update("DELETE FROM estaciones WHERE id = ?", estacion);
        assertThat(etagActual()).isNotEqualTo(conEstacionEditada);
    }

    @Test
    @DisplayName("HU-12: la base de datos rechaza un tramo que no sea una LineString con al menos dos puntos")
    void geometriaInvalidaSeRechaza() {
        assertThatThrownBy(() -> insertarTramo("Punto", 1, "{\"type\":\"Point\",\"coordinates\":[-76.61,2.46]}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarTramo("Corto", 2, "{\"type\":\"LineString\",\"coordinates\":[[-76.61,2.46]]}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarTramo("Sin coordenadas", 3, "{\"type\":\"LineString\"}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarTramo("Sin tipo", 4, "{\"coordinates\":[[-76.61,2.46],[-76.60,2.47]]}"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarTramo("Vacío", 5, "{}"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarTramo("Coordenadas en texto", 6, "{\"type\":\"LineString\",\"coordinates\":\"abc\"}"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-12: dos tramos no pueden tener el mismo orden")
    void ordenUnico() {
        insertarTramo("Tramo sur", 1, TRAMO_1);

        assertThatThrownBy(() -> insertarTramo("Otro tramo", 1, TRAMO_2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Utilidades

    private String etagActual() throws Exception {
        return mockMvc.perform(get(RUTA)).andExpect(status().isOk()).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
    }

    private void insertarTramo(String nombre, int orden, String geometria) {
        jdbc.update("INSERT INTO tramos_recorrido (id, nombre, orden, geometria) VALUES (?, ?, ?, CAST(? AS jsonb))",
                UUID.randomUUID(), nombre, orden, geometria);
    }

    private UUID insertarEstacion(String nombre, String latitud, String longitud) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO estaciones (id, nombre, descripcion, foto_clave, latitud, longitud) VALUES (?, ?, ?, ?, ?, ?)",
                id, nombre, "Descripción de " + nombre, "estaciones/" + id + ".png",
                new BigDecimal(latitud), new BigDecimal(longitud));
        return id;
    }
}
