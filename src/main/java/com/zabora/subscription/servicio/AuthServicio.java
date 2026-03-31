package com.zabora.subscription.servicio;

import com.zabora.subscription.excepcion.AuthServiceException;
import com.zabora.subscription.repositorio.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Llama al auth-service via Feign + Consul para actualizar roles cuando
 * se activa o cancela una suscripcion premium.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServicio {

    private final AuthClient authClient;

    /**
     * Sube el usuario a PREMIUM.
     * Llamado desde BricksPaymentServicio y WebhookPagoServicio tras pago aprobado.
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
     * Baja el usuario a GRATUITO (obligatorio para cancelación manual / admin).
     * Si falla la llamada a auth-service, lanza {@link AuthServiceException} y la transacción
     * de negocio puede revertirse (p. ej. no se confirma la cancelación sin degradar premium).
     */
    public void revertirAGratuito(Integer usuarioId) {
        log.info("Revirtiendo rol a GRATUITO en auth-service — usuario: {}", usuarioId);
        try {
            authClient.revertirAGratuito(usuarioId, Map.of("reason", "Suscripcion cancelada"));
            log.info("Rol GRATUITO actualizado correctamente — usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("No se pudo revertir rol a GRATUITO para usuario {}. Error: {}", usuarioId, e.getMessage(), e);
            throw new AuthServiceException(usuarioId, e.getMessage(), e);
        }
    }

    /**
     * Igual que {@link #revertirAGratuito(Integer)} pero no lanza (jobs programados / reintentos).
     */
    public void revertirAGratuitoSilencioso(Integer usuarioId) {
        try {
            revertirAGratuito(usuarioId);
        } catch (AuthServiceException e) {
            log.error("revertirAGratuitoSilencioso: omitido para usuario {} — {}", usuarioId, e.getMessage());
        }
    }
}
