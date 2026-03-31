package com.zabora.subscription.excepcion;

public class AuthServiceException extends RuntimeException {

    private final Integer usuarioId;

    public AuthServiceException(Integer usuarioId, String mensaje, Throwable causa) {
        super("Error actualizando rol en auth-service para usuario " + usuarioId + ": " + mensaje, causa);
        this.usuarioId = usuarioId;
    }

    public Integer getUsuarioId() { return usuarioId; }
}
