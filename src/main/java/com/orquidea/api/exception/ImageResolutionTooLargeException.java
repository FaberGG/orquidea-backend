package com.orquidea.api.exception;

/** La foto tiene más píxeles de los que se pueden procesar sin agotar la memoria. */
public class ImageResolutionTooLargeException extends RuntimeException {

    public ImageResolutionTooLargeException(int megapixelesMaximos) {
        super("La imagen supera la resolución máxima permitida de " + megapixelesMaximos + " megapíxeles.");
    }
}
