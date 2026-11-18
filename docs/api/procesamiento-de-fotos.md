# Procesamiento de fotos

Todas las fotos que recibe la API (fichas, estaciones, componentes y reportes de avistamiento) se optimizan antes de
guardarlas: quedan mucho más livianas, sin metadatos y con una miniatura para los listados. El contrato de subida no
cambia (mismos campos, jpg o png de hasta 10 MB). Las respuestas suman el campo `urlMiniatura`.

## Qué pasa al subir una foto

1. **Se valida.** Debe ser jpg o png (por su contenido, no por la extensión), pesar hasta 10 MB y tener como máximo
   50 megapíxeles. Además se decodifica completa: un archivo dañado que empiece como JPEG ya no se acepta.
2. **Se endereza.** Los celulares guardan la foto como sale del sensor y anotan en el EXIF cómo girarla. Se aplica
   esa orientación (los 8 valores posibles, también los espejados) y la foto se guarda ya derecha.
3. **Se reduce** a 1600 px de ancho, medido con la foto ya derecha: una foto vertical queda de 1600 × 2133. Las fotos
   más angostas no se agrandan. La reducción va por pasos de máximo la mitad, con interpolación bicúbica, para que
   las plumas, el follaje y otras texturas finas no se llenen de dientes de sierra ni de moiré.
4. **Se codifica:**

   | Foto subida | Se guarda como |
   |---|---|
   | JPEG | JPEG de calidad 85 con color completo (4:4:4), progresivo y con tablas Huffman optimizadas |
   | PNG con transparencia | PNG, sin pérdida y con el canal alfa intacto |
   | PNG sin transparencia | JPEG, igual que arriba |

   La extensión de la URL corresponde al formato guardado: un PNG sin transparencia queda en `.jpg`.
5. **Se le quitan los metadatos:** EXIF (ubicación GPS, modelo del teléfono, fecha), XMP, IPTC, comentarios y las
   imágenes extra que algunos celulares agregan al final del archivo. El bucket es público, así que antes cualquiera
   podía leer la ubicación exacta de quien tomó la foto.
6. **Se conserva el perfil de color (ICC).** No es un metadato personal: indica cómo interpretar los colores. Muchos
   celulares recientes fotografían en Display P3, y sin el perfil esas fotos se verían apagadas.
7. **No se recomprime sin necesidad.** Si la foto no hacía falta reducirla ni enderezarla y recomprimirla no la
   aligera al menos un 10 %, se guarda la original sin metadatos y con sus mismos datos de imagen, sin perder calidad.
8. **Se genera la miniatura:** 480 px de ancho, en el mismo formato y con el mismo perfil de color. Se guarda junto a
   la foto, en una subcarpeta `miniaturas/` (`fichas/abc.jpg` y `fichas/miniaturas/abc.jpg`).

El procesamiento tarda de 0,4 a 1,4 s por foto de 12 MP.

## Por qué calidad 85 con color 4:4:4

El JPEG puede guardar el color a la mitad de resolución que el brillo (4:2:0, lo habitual) o a la misma (4:4:4). Con
4:2:0, el color se corre en los bordes donde cambia mucho: un ave azul contra el follaje, los pétalos de una flor.
Como el valor de la plataforma está en mostrar bien especies que se distinguen por su color, se eligió la variante que
mejor conserva el color sin perder lo liviano.

Medición con cuatro fotos de Wikimedia Commons: dos azulejos (*Thraupis episcopus*) tomados con cámara, y un humedal
y una orquídea tomados con celular. Cada foto se redujo a 1600 px y se comparó cada variante contra esa misma
reducción sin comprimir.

| Variante | Peso medio | SSIM (brillo) | PSNR brillo | PSNR color |
|---|---|---|---|---|
| Calidad 80, 4:2:0 (sugerencia inicial) | 222 KB | 0,971 | 39,7 dB | 46,5 dB |
| Calidad 85, 4:2:0 | 265 KB | 0,977 | 41,1 dB | 47,1 dB |
| **Calidad 85, 4:4:4 (elegida)** | **315 KB** | **0,977** | **41,1 dB** | **49,6 dB** |
| Calidad 90, 4:4:4 | 406 KB | 0,983 | 43,2 dB | 50,4 dB |
| Calidad 95, 4:4:4 | 612 KB | 0,990 | 46,6 dB | 51,9 dB |

El SSIM va de 0 a 1 (1 es idéntica). El PSNR sube con la fidelidad: cada 3 dB más es la mitad del error.

- **El color completo es lo que más rinde.** A la misma calidad 85, pasar de 4:2:0 a 4:4:4 cuesta un 19 % más de peso
  y reduce el error de color en un 43 % (+2,4 dB). Subir la calidad de 85 a 90 cuesta un 29 % más y apenas mejora
  el color (+0,8 dB).
- **Calidad 85 frente a 80:** la nitidez sube 1,4 dB por un 20 % más de peso.
- **De 90 en adelante, cada mejora sale cara.** Calidad 95 pesa casi el doble que 85 por diferencias que no se ven al
  tamaño del detalle.

Resultado con el procesador real sobre las mismas fotos:

| Foto | Original | Guardada | Miniatura |
|---|---|---|---|
| Azulejo, cámara, 4383 × 2922 | 5,32 MB | 221 KB (4,1 %) | 34 KB |
| Azulejo, cámara, 3693 × 2462 | 5,69 MB | 197 KB (3,4 %) | 27 KB |
| Humedal, celular, 4160 × 2340 | 3,85 MB | 461 KB (11,7 %) | 48 KB |
| Orquídea, celular, 3024 × 4032 | 1,80 MB | 374 KB (20,3 %) | 55 KB |
| **Total** | **16,66 MB** | **1,22 MB (7,3 %)** | **164 KB** |

Las fotos guardadas pesan un 93 % menos. El humedal es la más pesada porque el follaje tiene mucho detalle, que es
justo lo que se quiere conservar. Las fotos que llegan del móvil, elegidas con `quality: 1`, pesan más que estas
originales, así que en la práctica el ahorro es mayor.

### Por qué no WebP

- WebP con pérdida siempre guarda el color a la mitad de resolución (4:2:0); a igual peso no supera al JPEG con 4:4:4.
- Java no trae codificador de WebP: habría que instalar bibliotecas nativas en la imagen de Docker.
- Los clientes leen las fotos directo del bucket, sin pasar por la API, así que no se puede entregar WebP solo a los
  que lo soportan.

## Miniaturas en las respuestas

| Respuesta | Foto | Miniatura |
|---|---|---|
| `TaxonDto` (ficha) | `urlFoto` (principal), `fotos[].url` | `urlMiniatura`, `fotos[].urlMiniatura` |
| `StationDto` (estación) | `urlFoto` | `urlMiniatura` |
| `StationTaxonDto` (ficha dentro de una estación) | `urlFoto` | `urlMiniatura` |
| `ComponentDto` (componente) | `fotos[].url` | `fotos[].urlMiniatura` |
| `OccurrenceDto` (reporte) | `urlFoto` | `urlMiniatura` (null si no hay foto) |

- Los listados (tarjetas, fichas de una estación, el foro) usan la miniatura; el detalle usa la foto.
- Una foto subida antes de las miniaturas y aún sin reprocesar devuelve en `urlMiniatura` la misma URL de la foto.
  Así el cliente no necesita un caso especial.
- **Sin conexión (HU-21):** la app puede guardar las miniaturas de todas las fichas y las fotos completas solo de las
  que se abren. Si `urlMiniatura` es igual a `url`, basta con descargarla una vez.

## Errores

| Situación | Estado | Mensaje |
|---|---|---|
| No es jpg ni png, o está dañada | 400 | El formato de la imagen no es válido. |
| Pesa más de 10 MB | 413 | La imagen supera el tamaño máximo permitido de 10 MB. |
| Tiene más de 50 megapíxeles | 413 | La imagen supera la resolución máxima permitida de 50 megapíxeles. |

## Configuración

En `application.yaml`, bajo `app.storage`:

| Propiedad | Variable de entorno | Por defecto | Qué hace |
|---|---|---|---|
| `imagen.ancho-maximo` | `STORAGE_IMAGEN_ANCHO_MAXIMO` | `1600` | Ancho de la foto guardada |
| `imagen.ancho-miniatura` | `STORAGE_IMAGEN_ANCHO_MINIATURA` | `480` | Ancho de la miniatura |
| `imagen.calidad-jpeg` | `STORAGE_IMAGEN_CALIDAD_JPEG` | `0.85` | Calidad JPEG, de 0 a 1 |
| `imagen.megapixeles-maximos` | — | `50` | Resolución máxima aceptada |
| `imagen.procesamientos-simultaneos` | — | `2` | Fotos que se procesan a la vez |
| `reprocesar-fotos` | `STORAGE_REPROCESAR_FOTOS` | `no` | `no`, `simular` o `aplicar` (ver abajo) |

Cambiar el ancho o la calidad solo afecta a las fotos que se suban después.

## Reprocesar las fotos existentes

Las fotos subidas antes de esta versión siguen como llegaron, con su EXIF y sin miniatura, hasta reprocesarlas. Se
hace una sola vez por entorno:

1. **Respaldar el bucket.** Aplicar reemplaza las fotos, y Cloudflare R2 no guarda versiones anteriores.
2. **Simular:** arrancar la API con `STORAGE_REPROCESAR_FOTOS=simular`. No escribe nada; al terminar, el log informa
   cuántas fotos se procesarían y cuánto se ahorraría:
   `Reprocesamiento de fotos terminado (simulación, no se cambió nada): 120 procesadas, 0 fallidas, 0 omitidas; 512.4 MB pasan a 38.9 MB (92 % menos)`.
3. **Aplicar:** arrancar con `STORAGE_REPROCESAR_FOTOS=aplicar`. Corre en segundo plano; la API atiende mientras tanto.
4. Volver a `STORAGE_REPROCESAR_FOTOS=no`.

Cómo funciona:

- Toma las fotos que no tienen miniatura y procesa cada una en su propia transacción. Si una falla (por ejemplo,
  porque el archivo ya no existe en el bucket), se registra en el log, queda pendiente y las demás siguen.
- Si la foto cambia, se guarda con una clave nueva y la anterior se borra cuando se confirma la transacción. Si ya
  estaba optimizada, conserva su clave y solo gana la miniatura.
- Las fichas y los componentes afectados cambian su fecha de modificación, así que la app sin conexión recibe las URLs
  nuevas en su siguiente sincronización (HU-21).
- Es idempotente: una segunda ejecución no vuelve a procesar las fotos que ya tienen miniatura. Si una foto se
  reemplaza o elimina mientras se procesa, se omite y se deshace lo que se subió para ella.

## Para el backend

- `storage/ImageProcessor` hace la optimización. `storage/PhotoStorage` la usa al subir y ata el borrado de la foto y
  su miniatura a la transacción. `JpegSegments` y `PngChunks` quitan los metadatos sin decodificar la imagen.
- El reprocesamiento está en `service/PhotoReprocessingService` y lo dispara `config/PhotoReprocessingInitializer`.
  La migración V18 agrega `miniatura_clave` a `fotos_ficha`, `estaciones`, `fotos_componente` y
  `reportes_avistamiento`.
- **Memoria:** decodificar una foto ocupa unos 4 bytes por píxel, unos 50 MB por foto de 12 MP. Por eso se limita la
  resolución y cuántas fotos se procesan a la vez, y el `Dockerfile` deja que la JVM use el 75 % de la memoria del
  contenedor (por defecto usa el 25 %).
- Para leer JPEG se usa TwelveMonkeys (`imageio-jpeg`), que abre archivos que el lector de la JDK rechaza (CMYK,
  algunos celulares). El resto es Java estándar (ImageIO y Java 2D).
- El procesamiento se prueba de punta a punta en `ImageProcessorTest` y el reprocesamiento en
  `PhotoReprocessingServiceTest`.
