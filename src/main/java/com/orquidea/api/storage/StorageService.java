package com.orquidea.api.storage;

/**
 * Almacenamiento de archivos públicos (fotos de fichas, etc.). El resto de capas solo conoce esta interfaz.
 */
public interface StorageService {

    void subir(String clave, byte[] contenido, String tipoContenido);

    void eliminar(String clave);

    /** URL desde la que el navegador puede leer el objeto sin autenticarse. */
    String urlPublica(String clave);

    /** Crea el bucket si no existe y le da lectura pública. Solo para desarrollo. */
    void asegurarBucketPublico();
}
