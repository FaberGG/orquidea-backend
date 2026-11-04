package com.orquidea.api.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

/**
 * Orden alfabético en español para los listados por nombre: "Águila" y "azulejo" antes que "Zorzal", y la "ñ" después
 * de la "n". No se ordena en la base de datos porque PostgreSQL en Alpine compara byte a byte, y ahí las mayúsculas
 * van antes que las minúsculas y las letras con tilde quedan al final.
 */
final class AlphabeticalOrder {

    private static final Collator ESPANOL = Collator.getInstance(Locale.forLanguageTag("es"));

    private AlphabeticalOrder() {
    }

    static <T> Comparator<T> por(Function<T, String> campo) {
        return Comparator.comparing(campo, ESPANOL);
    }
}
