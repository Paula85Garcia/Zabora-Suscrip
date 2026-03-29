package com.zabora.subscription.servicio;

import com.zabora.subscription.excepcion.AuthServiceException;
import com.zabora.subscription.repositorio.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Llama al auth-service via Feign + Consul para actualizar roles cuando
 * se activa o cancela una suscripcion premium.
 *
 * Consul resuelve "auth-service" a la instancia registrada.
 * Si el auth-service no esta disponible, se registra el error
 * pero NO se cancela la operacion de suscripcion (best-effort).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServicio {

    private final AuthClient authClient;

    /**
     * Sube el usuario a PREMIUM.
     * Llamado desde BricksPaymentServicio y WebhookPagoServicio tras pago aprobado.
     *
     * @param usuarioId ID del usuario en el auth-service
     */
    public void actualizarRolPremium(Integer usuarioId) {
        log.info("Actualizando rol a PREMIUM en auth-service — usuario: {}", usuarioId);
        try {
            authClient.actualizarRolPremium(usuarioId);
            log.info("Rol PREMIUM actualizado correctamente en auth-service — usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("No se pudo actualizar rol a PREMIUM para usuario {}. " +
                "auth-service puede estar caido. Error: {}", usuarioId, e.getMessage());
            throw new AuthServiceException(usuarioId, e.getMessage(), e);
        }
    }

    /**
     * Baja el usuario a GRATUITO.
     * Llamado cuando se cancela la suscripcion.
     *
     * @param usuarioId ID del usuario en el auth-service
     */
    public void revertirAGratuito(Integer usuarioId) {
        log.info("Revirtiendo rol a GRATUITO en auth-service — usuario: {}", usuarioId);
        try {
            authClient.revertirAGratuito(usuarioId);
            log.info("Rol GRATUITO actualizado correctamente — usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("No se pudo revertir rol a GRATUITO para usuario {}. Error: {}", usuarioId, e.getMessage());
            // No lanzar — operacion best-effort al cancelar
        }
    }
}
