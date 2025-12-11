package com.zabora.subscription.excepcion;

public class PagoException extends RuntimeException {
    public PagoException(String mensaje) {
        super(mensaje);
    }
}