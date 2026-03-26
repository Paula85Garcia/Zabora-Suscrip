package com.zabora.subscription.servicio;

import com.zabora.subscription.repositorio.AuthClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * Llama al auth-service via Feign para actualizar roles cuando
 * se activa o cancela una suscripcion premium.
 *
 * Si el auth-service no esta disponible, se registra el error
 * pero NO se cancela la operacion de suscripcion.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServicio {

    private static final Logger log = LoggerFactory.getLogger(AuthServicio.class);

    private final AuthClient authClient;

    public void actualizarRolPremium(Integer usuarioId) {
        try {
            authClient.actualizarRolPremium(usuarioId);
            log.info("Rol actualizado a PREMIUM en auth-service - Usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("No se pudo actualizar rol a PREMIUM para usuario {}. El auth-service puede estar caido. Error: {}",
                usuarioId, e.getMessage());
        }
    }

    public void revertirAGratuito(Integer usuarioId) {
        try {
            authClient.revertirAGratuito(usuarioId);
            log.info("Rol revertido a GRATUITO en auth-service - Usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("No se pudo revertir rol a GRATUITO para usuario {}. Error: {}",
                usuarioId, e.getMessage());
        }
    }
}