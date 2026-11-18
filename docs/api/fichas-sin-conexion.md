# Fichas taxonómicas sin conexión (HU-21)

Contrato para que la app descargue las fichas y las mantenga al día sin pedirlas una por una. La especificación
completa está en [`openapi.json`](openapi.json) y en Swagger UI (`/swagger-ui.html`), etiqueta *Fichas taxonómicas*.

El mapa del sendero ya se puede guardar sin conexión con `GET /api/mapa` y `If-None-Match` (HU-12). Sus
estaciones traen el `id` de cada ficha: al abrir una ficha desde el mapa sin conexión, se busca por ese `id` en la
copia local.

## Cambio de contrato: el listado ahora es paginado

`GET /api/fichas-taxonomicas` ya no devuelve un arreglo sino una página, con el mismo formato del foro de
avistamientos (`GET /api/reportes-avistamiento`):

```json
{
  "contenido": [ { "id": "…", "vernacularName": "Azulejo", "…": "…" } ],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 42,
  "totalPaginas": 3
}
```

| Parámetro | Por defecto | Notas |
|---|---|---|
| `categoria` | todas | `AVE`, `PLANTA`, `INSECTO`, `MAMIFERO`, `REPTIL`, `ANFIBIO` o `HONGO` |
| `pagina` | `0` | Desde 0; un valor negativo se toma como 0 |
| `tamano` | `20` | Se ajusta al rango de 1 a 50 |

Las fichas salen en orden alfabético por nombre común: sin importar mayúsculas ni tildes, y la "ñ" después de la
"n". Para seguir mostrando todo el listado, el cliente lee `contenido` y pide la página siguiente mientras
`pagina + 1 < totalPaginas`.

## Sincronización

`GET /api/fichas-taxonomicas/sincronizacion?modificadoDesde=…` es pública y no requiere token.

```json
{
  "fechaSincronizacion": "2026-11-10T15:30:00.123456Z",
  "completa": false,
  "fichas": [ { "id": "…", "fotos": [ { "id": "…", "url": "…", "urlMiniatura": "…" } ], "…": "…" } ],
  "idsEliminados": [ "5b0c7f0e-2f5d-4a8e-9a57-0c7d3c1b6e21" ]
}
```

Cada elemento de `fichas` tiene el mismo formato de `GET /api/fichas-taxonomicas/{id}` (`TaxonDto`).

### Flujo en la app

1. **Primera descarga**, o si se perdió la copia local: se llama sin `modificadoDesde`. La respuesta trae
   `completa: true` con todas las fichas, y la copia local se reemplaza entera.
2. Se guarda `fechaSincronizacion` tal como llegó, como texto.
3. **Actualizaciones**, cada vez que haya conexión: se envía `modificadoDesde=<fechaSincronizacion guardada>`.
   La respuesta trae `completa: false`, y entonces la app:
   - guarda cada ficha de `fichas` por su `id`, insertándola o reemplazándola;
   - borra de la copia local los ids de `idsEliminados`, e ignora los que no tenga;
   - guarda la nueva `fechaSincronizacion`.
4. Descarga las fotos nuevas desde `fotos[].url`, o solo sus miniaturas desde `fotos[].urlMiniatura` (ver
   [procesamiento de fotos](procesamiento-de-fotos.md#miniaturas-en-las-respuestas)), y borra las que ya no aparezcan
   en su ficha. La URL de una foto no cambia mientras la foto exista. La excepción es el reprocesamiento único de las
   fotos antiguas: ahí la ficha llega como modificada, con las URLs nuevas.

### Reglas

- Se envía el valor que devolvió el servidor, nunca la hora del teléfono, que puede estar desfasada.
- `modificadoDesde` va en ISO-8601 UTC (terminado en `Z`). Si se envía con zona horaria, el `+` va codificado
  como `%2B`. Un valor inválido responde 400: `El valor del parámetro 'modificadoDesde' no es válido.`
- **Puede llegar una ficha que ya se tenía.** El servidor repasa un minuto antes de `modificadoDesde`, para no
  perder una edición que se confirmó justo durante la sincronización anterior. Por eso las fichas se guardan por
  `id`, sin duplicarlas.
- Un mismo `id` nunca aparece a la vez en `fichas` y en `idsEliminados`, así que no importa cuál se aplique primero.
- Cuentan como cambios crear una ficha, editarla, agregarle o quitarle fotos, y reprocesar sus fotos.
- Las eliminaciones se registran aunque la ficha se borre directamente en la base de datos, y no vencen: una
  `fechaSincronizacion` antigua sigue sirviendo.
- La sincronización no se pagina: trae todo lo que cambió (en la primera descarga, todas las fichas).

### Mensajes de la HU

Los resuelve la app con su copia local; el backend no interviene:

- Con la información descargada y sin conexión, se muestran las fichas y el mapa guardados.
- Si la ficha o sección no se descargó: *Esta información no está disponible sin conexión. Conéctate a internet
  para descargarla.*

### Ejemplos

```bash
# Primera descarga
curl "http://localhost:8080/api/fichas-taxonomicas/sincronizacion"

# Actualización con la fecha recibida la vez anterior
curl "http://localhost:8080/api/fichas-taxonomicas/sincronizacion?modificadoDesde=2026-11-10T15:30:00.123456Z"
```

## Para el backend

- Las eliminaciones las registra un trigger en la tabla `fichas_eliminadas` (migración V17).
- El orden alfabético lo da la intercalación `es-x-icu` de la columna `nombre_comun` (V16). Requiere un
  PostgreSQL compilado con ICU, como la imagen oficial `postgres:alpine`.
- SCRUM-61 (modelo Darwin Core) cambiará los campos de `TaxonDto`, y con ellos los de este contrato.
