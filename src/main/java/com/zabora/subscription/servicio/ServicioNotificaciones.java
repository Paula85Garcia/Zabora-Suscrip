package com.zabora.subscription.servicio;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Alertas por correo a administradores (activación / cancelación de suscripciones).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioNotificaciones {

    private final JavaMailSender mailSender;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.from:noreply@zabora.com}")
    private String emailFrom;

    @Value("${email.admin-notifications-enabled:false}")
    private boolean adminNotificationsEnabled;

    @Value("${email.admin-recipients:}")
    private String adminRecipientsRaw;

    public void notificarPagoCompletado(Integer usuarioId, String suscripcionId) {
        notificarPremiumActivado(usuarioId, suscripcionId, "Pago completado (notificación interna)");
    }

    /** Compatibilidad con Webhook: mismo correo admin que {@link #notificarPremiumActivadoWebhook}. */
    public void notificarActualizacionSuscripcion(Integer usuarioId, String tipo) {
        if (!adminNotificationsEnabled || !StringUtils.hasText(adminRecipientsRaw)) {
            log.debug("Admin notify suscripcion omitido — usuario: {}, tipo: {}", usuarioId, tipo);
            return;
        }
        enviarCorreoAdmin(
            "[Zabora] Actualización de suscripción",
            "Usuario ID: " + usuarioId + "\nTipo / estado: " + tipo + "\n"
        );
    }

    /**
     * Premium activado tras pago aprobado (Bricks, respuesta inmediata de MP).
     */
    public void notificarPremiumActivadoBricks(Integer usuarioId, String suscripcionId, String mpPaymentId) {
        notificarPremiumActivado(
            usuarioId,
            suscripcionId,
            "Pago aprobado en tiempo real (Mercado Pago). MP payment id: " + mpPaymentId
        );
    }

    /**
     * Premium activado vía webhook (p. ej. PSE acreditado después).
     */
    public void notificarPremiumActivadoWebhook(Integer usuarioId, String suscripcionId, String mpPaymentId) {
        notificarPremiumActivado(
            usuarioId,
            suscripcionId,
            "Pago aprobado vía webhook. MP payment id: " + mpPaymentId
        );
    }

    private void notificarPremiumActivado(Integer usuarioId, String suscripcionId, String detalle) {
        if (!adminNotificationsEnabled || !StringUtils.hasText(adminRecipientsRaw)) {
            log.info("Premium activado — usuario: {}, suscripcion: {} (sin correo admin configurado)", usuarioId, suscripcionId);
            return;
        }
        enviarCorreoAdmin(
            "[Zabora] Suscripción premium activada",
            "Usuario ID: " + usuarioId + "\nSuscripción ID: " + suscripcionId + "\n" + detalle + "\n"
        );
    }

    public void notificarSuscripcionCancelada(
            Integer usuarioId,
            String suscripcionId,
            boolean inmediata,
            String origen) {
        if (!adminNotificationsEnabled || !StringUtils.hasText(adminRecipientsRaw)) {
            log.info("Suscripcion cancelada — usuario: {}, id: {}, inmediata: {}, origen: {} (sin correo admin)",
                usuarioId, suscripcionId, inmediata, origen);
            return;
        }
        enviarCorreoAdmin(
            "[Zabora] Suscripción cancelada",
            "Usuario ID: " + usuarioId
                + "\nSuscripción ID: " + suscripcionId
                + "\nCancelación inmediata: " + inmediata
                + "\nOrigen: " + origen + "\n"
        );
    }

    private void enviarCorreoAdmin(String subject, String textBody) {
        if (!emailEnabled) {
            log.info("[SIMULADO] Admin mail: {}\n{}", subject, textBody);
            return;
        }
        List<String> to = parseRecipients(adminRecipientsRaw);
        if (to.isEmpty()) {
            log.warn("email.admin-recipients vacío — no se envía: {}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(emailFrom);
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(textBody, false);
            mailSender.send(message);
            log.info("Correo admin enviado: {} → {}", subject, to);
        } catch (Exception e) {
            log.error("Error enviando correo admin ({}): {}", subject, e.getMessage());
        }
    }

    private static List<String> parseRecipients(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;]"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }
}
