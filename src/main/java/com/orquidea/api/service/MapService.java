package com.orquidea.api.service;

import com.orquidea.api.dto.MapDto;
import com.orquidea.api.dto.StationDto;
import com.orquidea.api.dto.TrailSegmentDto;
import com.orquidea.api.mapper.TrailSegmentMapper;
import com.orquidea.api.repository.TrailSegmentRepository;
import com.orquidea.api.repository.TrailSegmentRepository.Bounds;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * HU-12. Arma en una sola respuesta todo lo que necesita el mapa (trazado y estaciones) y le calcula una versión,
 * para que el cliente pueda guardarla y saber cuándo actualizarla (HE-08, uso sin conexión).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    /** Bytes del SHA-256 que se conservan en la versión: 8 bytes son 16 caracteres hexadecimales. */
    private static final int BYTES_DE_VERSION = 8;

    private final TrailSegmentRepository trailSegmentRepository;
    private final TrailSegmentMapper trailSegmentMapper;
    private final StationService stationService;

    public MapDto obtener() {
        List<TrailSegmentDto> tramos = trailSegmentRepository.findAll(Sort.by("orden")).stream()
                .map(trailSegmentMapper::aDto)
                .toList();
        List<StationDto> estaciones = stationService.listar();

        MapDto mapa = MapDto.builder()
                .limites(calcularLimites(estaciones))
                .tramos(tramos)
                .estaciones(estaciones)
                .build();
        mapa.setVersion(calcularVersion(mapa));
        return mapa;
    }

    /**
     * [minLon, minLat, maxLon, maxLat] que contiene el trazado y también las estaciones, para que ninguna quede
     * fuera de la vista inicial. Los tramos se miden en la base de datos; las estaciones, aquí.
     */
    private List<Double> calcularLimites(List<StationDto> estaciones) {
        Bounds tramos = trailSegmentRepository.calcularLimites();
        List<Double> longitudes = new ArrayList<>();
        List<Double> latitudes = new ArrayList<>();
        if (tramos.getMinLon() != null) {
            longitudes.add(tramos.getMinLon());
            longitudes.add(tramos.getMaxLon());
            latitudes.add(tramos.getMinLat());
            latitudes.add(tramos.getMaxLat());
        }
        for (StationDto estacion : estaciones) {
            longitudes.add(valor(estacion.getDecimalLongitude()));
            latitudes.add(valor(estacion.getDecimalLatitude()));
        }
        if (longitudes.isEmpty()) {
            return null;
        }
        return List.of(
                longitudes.stream().min(Double::compare).orElseThrow(),
                latitudes.stream().min(Double::compare).orElseThrow(),
                longitudes.stream().max(Double::compare).orElseThrow(),
                latitudes.stream().max(Double::compare).orElseThrow());
    }

    /**
     * Huella del contenido. Se calcula sobre toString() de la respuesta, que Lombok genera con todos los campos
     * (con la versión aún nula): si alguien agrega un campo al mapa o a las estaciones, la versión lo cubre sin
     * tocar este método. Es determinista porque las listas llegan ordenadas.
     */
    private static String calcularVersion(MapDto mapa) {
        try {
            byte[] huella = MessageDigest.getInstance("SHA-256")
                    .digest(mapa.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(huella, 0, BYTES_DE_VERSION);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", ex);
        }
    }

    private static double valor(BigDecimal coordenada) {
        return coordenada.doubleValue();
    }
}
