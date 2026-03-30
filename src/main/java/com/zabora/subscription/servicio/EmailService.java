package com.zabora.subscription.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zabora.subscription.modelo.entidad.Pago;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Correo transaccional (confirmación de pago, factura / comprobante solicitado por el usuario).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JavaMailSender mailSender;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.send-factura:true}")
    private boolean sendFactura;

    @Value("${email.from:noreply@zabora.com}")
    private String emailFrom;

    /**
     * Envía confirmación de pago exitoso.
     */
    public void enviarConfirmacionPago(Pago pago) {
        if (!emailEnabled) {
            log.info("[SIMULADO] Confirmacion de pago — usuario: {}", pago.getUsuarioId());
            return;
        }
        String to = resolverEmailDestinoFactura(pago);
        if (!StringUtils.hasText(to)) {
            log.warn("Sin email destino para confirmación de pago {}", pago.getId());
            return;
        }
        String cuerpo = construirMensajeConfirmacion(pago);
        enviarCorreo(to, "[Zabora] Pago confirmado", cuerpo, false);
    }

    /**
     * Comprobante / factura simple por correo (sin PDF hasta integrar plantilla o MP).
     */
    public void enviarFacturaPago(Pago pago) {
        if (!emailEnabled || !sendFactura) {
            log.info("[SIMULADO] Factura/comprobante — pago: {}, usuario: {}", pago.getId(), pago.getUsuarioId());
            return;
        }
        String to = resolverEmailDestinoFactura(pago);
        if (!StringUtils.hasText(to)) {
            log.warn("Usuario solicitó factura pero no hay payerEmail en metadatos del pago {}", pago.getId());
            return;
        }
        String cuerpo = construirCuerpoFactura(pago);
        enviarCorreo(to, "[Zabora] Comprobante de pago — Suscripción Premium", cuerpo, false);
    }

    private void enviarCorreo(String to, String subject, String text, boolean html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(emailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
            log.info("Correo enviado: {} → {}", subject, to);
        } catch (Exception e) {
            log.error("Error enviando correo ({}): {}", subject, e.getMessage());
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    private static String resolverEmailDestinoFactura(Pago pago) {
        String m = pago.getMetadatos();
        if (!StringUtils.hasText(m)) {
            return null;
        }
        try {
            JsonNode n = JSON.readTree(m);
            JsonNode em = n.get("payerEmail");
            if (em != null && em.isTextual() && StringUtils.hasText(em.asText())) {
                return em.asText().trim();
            }
        } catch (Exception e) {
            log.debug("Metadatos de pago no JSON o sin payerEmail: {}", e.getMessage());
        }
        return null;
    }

    private String construirMensajeConfirmacion(Pago pago) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format(
            "Hola,\n\nTu pago fue confirmado.\n\nID pago: %s\nMonto: %s %s\nFecha: %s\n\nGracias por usar Zabora.\n",
            pago.getId(),
            pago.getMonto(),
            pago.getMoneda(),
            pago.getFechaPago() != null ? pago.getFechaPago().format(fmt) : "N/A"
        );
    }

    private String construirCuerpoFactura(Pago pago) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format(
            """
            Hola,

            Este es tu comprobante de pago por la suscripción premium Zabora.

            ID de pago: %s
            Referencia suscripción: %s
            Monto: %s %s
            Método: %s
            Fecha: %s
            %s

            Conserva este correo como respaldo. Si necesitas factura electrónica de venta, indícalo a soporte con este ID.

            —
            Zabora
            """,
            pago.getId(),
            pago.getSuscripcionId(),
            pago.getMonto(),
            pago.getMoneda(),
            pago.getMetodoPago(),
            pago.getFechaPago() != null ? pago.getFechaPago().format(fmt) : "N/A",
            StringUtils.hasText(pago.getCodigoAutorizacion())
                ? "Código autorización: " + pago.getCodigoAutorizacion()
                : ""
        );
    }
}
