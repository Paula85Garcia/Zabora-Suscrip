package com.zabora.subscription.servicio;

import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.zabora.subscription.modelo.dto.CrearPagoBricksRequest;
import com.zabora.subscription.modelo.dto.CrearPagoBricksResponse;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthServicio authServicio;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${mercadopago.webhook.notification-url}")
    private String notificationUrl;

    @Value("${mercadopago.success-url}")
    private String successUrl;

    @Value("${mercadopago.failure-url}")
    private String failureUrl;

    @Value("${mercadopago.pending-url}")
    private String pendingUrl;

    /**
     * Crea una preferencia de pago en MercadoPago y registra el intento en BD.
     */
    @Transactional
    public CrearPagoBricksResponse crearPreferencia(CrearPagoBricksRequest request, Integer usuarioId) {
        log.info("Creando preferencia - Suscripcion: {}, Usuario: {}", request.getIdSuscripcion(), usuarioId);

        UsuarioSuscripcion suscripcion = suscripcionRepositorio
            .findById(request.getIdSuscripcion())
            .orElseThrow(() -> new IllegalStateException("Suscripcion no encontrada: " + request.getIdSuscripcion()));

        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("Esta suscripcion no pertenece al usuario");
        }

        if (suscripcion.getEstado() != EstadoSuscripcion.PENDIENTE_PAGO) {
            throw new IllegalStateException(
                "La suscripcion no esta en PENDIENTE_PAGO. Estado actual: " + suscripcion.getEstado());
        }

        // Si ya existe pago pendiente, retornarlo sin crear uno nuevo
        boolean hayPagoPendiente = pagoRepositorio
            .existsBySuscripcionIdAndEstado(request.getIdSuscripcion(), EstadoPago.PENDIENTE);

        if (hayPagoPendiente) {
            Pago pagoExistente = pagoRepositorio
                .findBySuscripcionIdAndEstado(request.getIdSuscripcion(), EstadoPago.PENDIENTE)
                .orElseThrow();
            log.info("Retornando pago pendiente existente: {}", pagoExistente.getId());
            return CrearPagoBricksResponse.builder()
                .preferenceId(pagoExistente.getIdIntentoPago())
                .publicKey(publicKey)
                .amount(pagoExistente.getMonto())
                .currency(pagoExistente.getMoneda())
                .subscriptionId(request.getIdSuscripcion())
                .paymentId(pagoExistente.getId())
                .build();
        }

        try {
            PreferenceClient client = new PreferenceClient();

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Suscripcion Premium Zabora")
                .description("Acceso completo por 30 dias")
                .quantity(1)
                .unitPrice(request.getMonto())
                .currencyId("COP")
                .build();

            Map<String, Object> metadata = Map.of(
                "suscripcion_id", request.getIdSuscripcion(),
                "usuario_id", String.valueOf(usuarioId)
            );

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build())
                .notificationUrl(notificationUrl)
                .metadata(metadata)
                .build();

            Preference preference = client.create(preferenceRequest);
            log.info("Preferencia creada en MercadoPago: {}", preference.getId());

            // Guardar pago en BD con estado PENDIENTE
            String pagoId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

            Pago pago = new Pago();
            pago.setId(pagoId);
            pago.setSuscripcionId(request.getIdSuscripcion());
            pago.setUsuarioId(usuarioId);
            pago.setMonto(request.getMonto());
            pago.setMoneda("COP");
            pago.setMetodoPago(resolverMetodoPago(request.getTipoPago()));
            pago.setEstado(EstadoPago.PENDIENTE);
            pago.setIdIntentoPago(preference.getId());
            pagoRepositorio.save(pago);

            log.info("Pago registrado en BD: {}", pagoId);

            return CrearPagoBricksResponse.builder()
                .preferenceId(preference.getId())
                .publicKey(publicKey)
                .amount(request.getMonto())
                .currency("COP")
                .subscriptionId(request.getIdSuscripcion())
                .paymentId(pagoId)
                .build();

        } catch (MPApiException e) {
            log.error("Error API MercadoPago - Status: {}, Body: {}",
                e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Error de MercadoPago: " + e.getMessage());
        } catch (MPException e) {
            log.error("Error SDK MercadoPago: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con MercadoPago: " + e.getMessage());
        }
    }

    /**
     * Verifica el estado de un pago en MercadoPago.
     */
    public Map<String, Object> procesarPago(Map<String, Object> paymentData, Integer usuarioId) {
        log.info("Verificando pago - Usuario: {}", usuarioId);

        String mpPaymentId = extraerPaymentId(paymentData);
        if (mpPaymentId == null) {
            throw new IllegalStateException("No se encontro payment_id en los datos");
        }

        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(mpPaymentId));
            String status = payment.getStatus();
            log.info("Pago {} - Status: {}", mpPaymentId, status);

            return Map.of(
                "success", "approved".equals(status),
                "status", status,
                "paymentId", mpPaymentId,
                "message", traducirEstado(status)
            );

        } catch (MPApiException e) {
            log.error("Error API MercadoPago: {}", e.getStatusCode());
            throw new RuntimeException("Error verificando pago: " + e.getMessage());
        } catch (MPException e) {
            log.error("Error SDK MercadoPago: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con MercadoPago: " + e.getMessage());
        }
    }

    /**
     * Activa la suscripcion despues de pago aprobado por webhook.
     */
    @Transactional
    public void activarSuscripcionPorPago(String suscripcionId, Integer usuarioId, Payment mpPayment) {
        log.info("Activando suscripcion: {} para usuario: {}", suscripcionId, usuarioId);

        pagoRepositorio.findBySuscripcionIdAndEstado(suscripcionId, EstadoPago.PENDIENTE)
            .ifPresent(pago -> {
                pago.setEstado(EstadoPago.COMPLETADO);
                pago.setIdIntentoPago(mpPayment.getId().toString());
                if (mpPayment.getAuthorizationCode() != null) {
                    pago.setCodigoAutorizacion(mpPayment.getAuthorizationCode());
                }
                pago.setFechaPago(LocalDateTime.now());
                pagoRepositorio.save(pago);
                log.info("Pago actualizado a COMPLETADO");
            });

        suscripcionRepositorio.findById(suscripcionId).ifPresent(suscripcion -> {
            if (suscripcion.getEstado() == EstadoSuscripcion.PENDIENTE_PAGO) {
                LocalDateTime ahora = LocalDateTime.now();
                suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
                suscripcion.setInicioPeriodoActual(ahora);
                suscripcion.setFinPeriodoActual(ahora.plusDays(30));
                suscripcion.setFechaActualizacion(ahora);
                suscripcionRepositorio.save(suscripcion);
                log.info("Suscripcion activada: {}", suscripcionId);
            }
        });

        authServicio.actualizarRolPremium(usuarioId);
    }

    /**
     * Marca el pago como fallido.
     */
    @Transactional
    public void marcarPagoFallido(String suscripcionId) {
        pagoRepositorio.findBySuscripcionIdAndEstado(suscripcionId, EstadoPago.PENDIENTE)
            .ifPresent(pago -> {
                pago.setEstado(EstadoPago.FALLIDO);
                pagoRepositorio.save(pago);
                log.info("Pago marcado como FALLIDO para suscripcion: {}", suscripcionId);
            });
    }

    // ---- helpers privados ----

    private String resolverMetodoPago(String tipoPago) {
        if (tipoPago == null) return "TARJETA_CREDITO";
        return switch (tipoPago.toLowerCase()) {
            case "pse", "bank_transfer" -> "PSE";
            default -> "TARJETA_CREDITO";
        };
    }

    private String extraerPaymentId(Map<String, Object> data) {
        if (data.containsKey("payment_id")) return String.valueOf(data.get("payment_id"));
        if (data.containsKey("id")) return String.valueOf(data.get("id"));
        return null;
    }

    private String traducirEstado(String status) {
        return switch (status) {
            case "approved" -> "Pago aprobado";
            case "pending", "in_process" -> "Pago pendiente de confirmacion";
            case "rejected" -> "Pago rechazado";
            case "cancelled" -> "Pago cancelado";
            default -> "Estado: " + status;
        };
    }
}