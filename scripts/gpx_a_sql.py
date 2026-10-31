#!/usr/bin/env python3
"""Convierte el GPX del recorrido en una migración Flyway que carga los tramos (HU-12).

Cada <trkseg> del GPX (o cada <rte> si no hay tracks) se vuelve un tramo: se pasa a GeoJSON LineString,
se simplifica con Douglas-Peucker y se escribe como INSERT en tramos_recorrido.

Uso:
    python3 scripts/gpx_a_sql.py recorrido.gpx --nombre "Sendero principal" \\
        > src/main/resources/db/migration/V14__cargar_trazado_recorrido.sql

Para sumar más tramos después (p. ej. el recorrido entre humedales) se genera otra migración con
--orden-inicial mayor que el último orden ya cargado. Los ids son deterministas (uuid5 sobre nombre y orden),
así que regenerar el mismo GPX produce el mismo SQL.
"""
import argparse
import math
import sys
import uuid
import json
import xml.etree.ElementTree as ET

ESPACIO_IDS = uuid.UUID("5d0f2a3e-7c1b-4e58-9b6a-0a1f2c3d4e5f")  # fijo: no cambiar, define los ids
METROS_POR_GRADO_LAT = 110_540.0
METROS_POR_GRADO_LON = 111_320.0
DECIMALES = 6  # ~0,1 m


def leer_segmentos(ruta):
    """Devuelve [(nombre_base, [(lon, lat), ...]), ...] a partir de tracks o, si no hay, de rutas."""
    raiz = ET.parse(ruta).getroot()
    segmentos = []
    for pista in raiz.iterfind("{*}trk"):
        nombre = (pista.findtext("{*}name") or "").strip()
        for tramo in pista.iterfind("{*}trkseg"):
            puntos = [(float(p.get("lon")), float(p.get("lat"))) for p in tramo.iterfind("{*}trkpt")]
            segmentos.append((nombre, puntos))
    if not segmentos:
        for ruta_gpx in raiz.iterfind("{*}rte"):
            nombre = (ruta_gpx.findtext("{*}name") or "").strip()
            puntos = [(float(p.get("lon")), float(p.get("lat"))) for p in ruta_gpx.iterfind("{*}rtept")]
            segmentos.append((nombre, puntos))
    return segmentos


def quitar_repetidos(puntos):
    resultado = []
    for punto in puntos:
        if not resultado or punto != resultado[-1]:
            resultado.append(punto)
    return resultado


def distancia_a_segmento(p, a, b):
    """Distancia en metros de p al segmento ab; los puntos son (x, y) ya en metros."""
    dx, dy = b[0] - a[0], b[1] - a[1]
    largo2 = dx * dx + dy * dy
    if largo2 == 0:
        return math.hypot(p[0] - a[0], p[1] - a[1])
    t = max(0.0, min(1.0, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / largo2))
    return math.hypot(p[0] - (a[0] + t * dx), p[1] - (a[1] + t * dy))


def douglas_peucker(puntos, tolerancia_m):
    """Simplifica la línea conservando el primer y el último punto. Iterativo, para tracks muy largos."""
    if len(puntos) < 3:
        return list(puntos)
    lat0 = sum(p[1] for p in puntos) / len(puntos)
    k = math.cos(math.radians(lat0))
    metros = [(p[0] * METROS_POR_GRADO_LON * k, p[1] * METROS_POR_GRADO_LAT) for p in puntos]
    conservar = [False] * len(puntos)
    conservar[0] = conservar[-1] = True
    pila = [(0, len(puntos) - 1)]
    while pila:
        inicio, fin = pila.pop()
        mayor, indice = 0.0, None
        for i in range(inicio + 1, fin):
            d = distancia_a_segmento(metros[i], metros[inicio], metros[fin])
            if d > mayor:
                mayor, indice = d, i
        if indice is not None and mayor > tolerancia_m:
            conservar[indice] = True
            pila.append((inicio, indice))
            pila.append((indice, fin))
    return [p for p, c in zip(puntos, conservar) if c]


def sql_texto(valor):
    return "'" + valor.replace("'", "''") + "'"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("gpx", help="archivo GPX del recorrido")
    parser.add_argument("--nombre", default="Sendero", help="nombre base de los tramos si el GPX no trae uno")
    parser.add_argument("--tolerancia", type=float, default=3.0,
                        help="tolerancia de Douglas-Peucker en metros (por defecto 3)")
    parser.add_argument("--orden-inicial", type=int, default=1, help="orden del primer tramo (por defecto 1)")
    args = parser.parse_args()

    segmentos = [(n, quitar_repetidos(p)) for n, p in leer_segmentos(args.gpx)]
    segmentos = [(n, p) for n, p in segmentos if len(p) >= 2]
    if not segmentos:
        sys.exit("El GPX no trae ningún track ni ruta con al menos 2 puntos.")

    # Si varios tramos comparten nombre (o no traen), se numeran para distinguirlos en el mapa
    nombres_base = [n or args.nombre for n, _ in segmentos]
    repetidos = {n for n in nombres_base if nombres_base.count(n) > 1}

    filas, total_antes, total_despues = [], 0, 0
    for posicion, (nombre_gpx, puntos) in enumerate(segmentos):
        simplificados = douglas_peucker(puntos, args.tolerancia)
        total_antes += len(puntos)
        total_despues += len(simplificados)
        orden = args.orden_inicial + posicion
        nombre = nombres_base[posicion]
        if nombre in repetidos:
            nombre = f"{nombre} {posicion + 1}"
        geometria = {"type": "LineString",
                     "coordinates": [[round(lon, DECIMALES), round(lat, DECIMALES)] for lon, lat in simplificados]}
        id_tramo = uuid.uuid5(ESPACIO_IDS, f"{nombre}|{orden}")
        filas.append(f"    ({sql_texto(str(id_tramo))}, {sql_texto(nombre)}, {orden}, "
                     f"{sql_texto(json.dumps(geometria, separators=(',', ':')))}::jsonb)")

    print(f"-- HU-12: trazado del recorrido. Generado con scripts/gpx_a_sql.py ({len(filas)} tramo(s), "
          f"tolerancia {args.tolerancia:g} m, {total_despues} de {total_antes} puntos).")
    print("INSERT INTO tramos_recorrido (id, nombre, orden, geometria) VALUES")
    print(",\n".join(filas) + ";")


if __name__ == "__main__":
    main()
