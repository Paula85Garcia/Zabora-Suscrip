package com.zabora.subscription.servicio;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio que procesa los eventos de pago recibidos via webhook de MercadoPago.
 *
 * Flujo:
 *  1. Recibe el ID del pago de MercadoPago
 *  2. Consulta los detalles del pago en la API de MercadoPago
 *  3. Busca el pago local en BD usando external_reference (= suscripcionId)
 *  4. Actualiza el estado del pago y la suscripcion segun el resultado
 *
 * Este servicio es llamado exclusivamente desde WebhookController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPagoServicio {

    private static final String META_RECIBIR_FACTURA = "\"recibirFactura\":true";

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthServicio authServicio;
    private final ServicioNotificaciones servicioNotificaciones;
    private final EmailService emailService;

    /**
     * Procesa un evento de pago recibido de MercadoPago.
     *
     * @param mpPaymentId ID del pago en MercadoPago (viene del webhook)
     */
    public void procesarEventoPago(String mpPaymentId) {
        log.info("Procesando evento de pago — MP Payment ID: {}", mpPaymentId);

        try {
            // 1. Consultar pago en MercadoPago
            PaymentClient client = new PaymentClient();
            Payment mpPayment = client.get(Long.parseLong(mpPaymentId));

            log.info("Pago MP consultado — ID: {}, Status: '{}', ExternalRef: '{}'",
                mpPayment.getId(), mpPayment.getStatus(), mpPayment.getExternalReference());

            // 2. Buscar pago local por idIntentoPago (el MP payment ID guardado al crear)
            Optional<Pago> pagoOpt = pagoRepositorio.findByIdIntentoPago(mpPaymentId);

            if (pagoOpt.isEmpty()) {
                // Intentar buscar por external_reference + estado PENDIENTE
                String externalRef = mpPayment.getExternalReference();
                if (externalRef != null) {
                    pagoOpt = pagoRepositorio.findBySuscripcionIdAndEstado(externalRef, EstadoPago.PENDIENTE);
                }
            }

            if (pagoOpt.isEmpty()) {
                log.warn("No se encontro pago local para MP Payment ID: {} — posiblemente ya fue procesado", mpPaymentId);
                return;
            }

            Pago pagoLocal = pagoOpt.get();

            // 3. Si ya esta en estado final, no reprocesar (idempotencia)
            if (pagoLocal.getEstado() == EstadoPago.COMPLETADO
                    || pagoLocal.getEstado() == EstadoPago.REEMBOLSADO) {
                log.info("Pago {} ya en estado final '{}' — ignorando webhook duplicado",
                    pagoLocal.getId(), pagoLocal.getEstado());
                return;
            }

            // 4. Actualizar segun estado de MercadoPago
            actualizarPagoSegunEstado(pagoLocal, mpPayment);

        } catch (NumberFormatException e) {
            log.error("MP Payment ID no es numerico: '{}'", mpPaymentId);
        } catch (MPApiException e) {
            log.error("Error consultando pago en MercadoPago — HTTP {}: {}",
                e.getStatusCode(), e.getApiResponse() != null ? e.getApiResponse().getContent() : "sin detalle");
        } catch (MPException e) {
            log.error("Error SDK MercadoPago: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error procesando evento de pago {}: {}", mpPaymentId, e.getMessage(), e);
        }
    }

    @Transactional
    protected void actualizarPagoSegunEstado(Pago pago, Payment mpPayment) {
        String mpStatus = mpPayment.getStatus();
        log.info("Actualizando pago {} segun estado de MercadoPago: '{}'", pago.getId(), mpStatus);

        switch (mpStatus) {
            case "approved" -> {
                log.info("Pago APROBADO via webhook");

                pago.setEstado(EstadoPago.COMPLETADO);
                pago.setIdIntentoPago(mpPayment.getId().toString());
                if (mpPayment.getAuthorizationCode() != null) {
                    pago.setCodigoAutorizacion(mpPayment.getAuthorizationCode());
                }
                pago.setFechaPago(LocalDateTime.now());
                pagoRepositorio.save(pago);

                log.info("Pago {} actualizado a COMPLETADO", pago.getId());

                if (usuarioSolicitaFacturaPorCorreo(pago)) {
                    try {
                        emailService.enviarFacturaPago(pago);
                    } catch (Exception e) {
                        log.warn("No se pudo enviar factura por correo (pago {}): {}", pago.getId(), e.getMessage());
                    }
                }

                activarSuscripcion(
                    pago.getSuscripcionId(),
                    pago.getUsuarioId(),
                    mpPayment.getId() != null ? mpPayment.getId().toString() : "");
            }

            case "pending", "in_process" -> {
                log.info("Pago {} sigue en estado '{}' — sin cambios", pago.getId(), mpStatus);
            }

            case "rejected", "cancelled" -> {
                log.info("Pago FALLIDO via webhook");
                pago.setEstado(EstadoPago.FALLIDO);
                if (mpPayment.getStatusDetail() != null) {
                    pago.setMotivoFallo(mpPayment.getStatusDetail());
                }
                pagoRepositorio.save(pago);
            }

            case "refunded", "charged_back" -> {
                log.info("Pago REEMBOLSADO via webhook");
                pago.setEstado(EstadoPago.REEMBOLSADO);
                pagoRepositorio.save(pago);
            }

            default -> log.warn("Estado de MercadoPago desconocido: '{}'", mpStatus);
        }
    }

    private static boolean usuarioSolicitaFacturaPorCorreo(Pago pago) {
        String m = pago.getMetadatos();
        return m != null && m.contains(META_RECIBIR_FACTURA);
    }

    private void activarSuscripcion(String suscripcionId, Integer usuarioId, String mpPaymentId) {
        try {
            Optional<UsuarioSuscripcion> suscripcionOpt = suscripcionRepositorio.findById(suscripcionId);

            if (suscripcionOpt.isEmpty()) {
                log.error("Suscripcion no encontrada: {}", suscripcionId);
                return;
            }

            UsuarioSuscripcion suscripcion = suscripcionOpt.get();

            log.info("Activando suscripcion {} — estado actual: {}, plan: {}",
                suscripcionId, suscripcion.getEstado(), suscripcion.getPlan().getNombre());

            // Solo activar si esta PENDIENTE_PAGO (idempotencia)
            if (suscripcion.getEstado() == EstadoSuscripcion.PENDIENTE_PAGO) {
                LocalDateTime now = LocalDateTime.now();
                suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
                suscripcion.setInicioPeriodoActual(now);
                suscripcion.setFinPeriodoActual(now.plusDays(30));
                suscripcion.setFechaActualizacion(now);
                suscripcionRepositorio.save(suscripcion);
                log.info("Suscripcion {} activada exitosamente", suscripcionId);

                // Actualizar rol en auth-service
                try {
                    authServicio.actualizarRolPremium(usuarioId);
                } catch (Exception e) {
                    log.error("Suscripcion {} activa pero fallo auth-service para usuario {}: {}",
                        suscripcionId, usuarioId, e.getMessage());
                }

                try {
                    servicioNotificaciones.notificarPremiumActivadoWebhook(usuarioId, suscripcionId, mpPaymentId);
                } catch (Exception e) {
                    log.warn("No se pudo notificar admin activación premium (webhook): {}", e.getMessage());
                }

            } else {
                log.info("Suscripcion {} ya estaba en estado: {} — no se activa de nuevo",
                    suscripcionId, suscripcion.getEstado());
            }

        } catch (Exception e) {
            log.error("Error activando suscripcion {}: {}", suscripcionId, e.getMessage(), e);
        }
    }
}
