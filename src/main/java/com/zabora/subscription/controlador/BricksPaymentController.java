package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.excepcion.PagoRechazadoException;
import com.zabora.subscription.modelo.dto.BricksPaymentDTO;
import com.zabora.subscription.servicio.BricksPaymentServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints del flujo de pago con MercadoPago Checkout Bricks.
 *
 * Expone:
 *   GET  /api/pagos/bricks/public-key  → public key para inicializar el Brick en frontend
 *   POST /api/pagos/bricks/pay         → procesa el pago con el token del Brick
 *
 * Los endpoints /preference y /process del PagoBricksController anterior
 * quedan ELIMINADOS — pertenecian al flujo viejo de Checkout Pro.
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos/bricks")
@RequiredArgsConstructor
@Tag(name = "Pagos Bricks", description = "Flujo de pago directo con MercadoPago Checkout Bricks")
public class BricksPaymentController {

    private final BricksPaymentServicio bricksPaymentServicio;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    /**
     * Devuelve la public key de MercadoPago para inicializar el Brick en el frontend.
     */
    @GetMapping("/public-key")
    @Operation(summary = "Obtener public key de MercadoPago")
    public ResponseEntity<Map<String, String>> obtenerPublicKey() {
        log.info("Solicitud de public key");
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    /**
     * Procesa el pago con el token capturado por el Brick.
     *
     * El frontend llama a este endpoint DESPUES de que el Brick tokenizo la tarjeta.
     * El PAN (numero de tarjeta) nunca llega al backend — solo el token.
     *
     * Body esperado:
     * {
     *   "token":               "ff8080814c11e237...",
     *   "paymentMethodId":     "visa",
     *   "issuerId":            "24",        <- puede ser null
     *   "installments":        1,
     *   "payerEmail":          "user@test.com",
     *   "externalReference":   "uuid-de-la-suscripcion-en-mysql",
     *   "transactionAmount":   29900.00,
     *   "description":         "Suscripcion Premium Zabora"
     * }
     */
    @PostMapping("/pay")
    @Operation(
        summary = "Procesar pago con Bricks",
        description = "Recibe el token del Brick, crea el pago en MercadoPago " +
                      "y activa la suscripcion si es aprobado en tiempo real."
    )
    public ResponseEntity<?> procesarPago(@Valid @RequestBody BricksPaymentDTO dto) {
        Integer usuarioId = obtenerUsuarioId();
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }

        log.info("Procesando pago Bricks — usuario: {}, suscripcion: {}",
            usuarioId, dto.getExternalReference());

        try {
            Map<String, Object> resultado = bricksPaymentServicio.procesarPagoConBricks(dto, usuarioId);
            return ResponseEntity.ok(resultado);

        } catch (PagoRechazadoException e) {
            log.warn("Pago rechazado — usuario: {}, detalle: '{}'", usuarioId, e.getStatusDetail());
            return ResponseEntity.status(422).body(Map.of(
                "error",       "Pago rechazado",
                "statusDetail", e.getStatusDetail(),
                "mpPaymentId",  e.getMpPaymentId(),
                "message",      traducirRechazo(e.getStatusDetail())
            ));

        } catch (SecurityException e) {
            log.warn("Acceso denegado — usuario: {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Estado invalido — usuario: {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error inesperado — usuario: {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno al procesar el pago. Intenta nuevamente."
            ));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Integer obtenerUsuarioId() {
        var userData = UserContext.get();
        return (userData != null) ? userData.getUserId() : null;
    }

    /**
     * Traduce los status_detail de MP a mensajes amigables en espanol.
     * Referencia: https://www.mercadopago.com.co/developers/es/docs/checkout-api/response-handling/collection-results
     */
    private String traducirRechazo(String statusDetail) {
        if (statusDetail == null) return "Tu pago no pudo ser procesado. Intenta con otro medio de pago.";
        return switch (statusDetail) {
            case "cc_rejected_bad_filled_card_number"   -> "El numero de tarjeta es incorrecto.";
            case "cc_rejected_bad_filled_date"          -> "La fecha de vencimiento es incorrecta.";
            case "cc_rejected_bad_filled_security_code" -> "El codigo de seguridad (CVV) es incorrecto.";
            case "cc_rejected_bad_filled_other"         -> "Revisa los datos de tu tarjeta.";
            case "cc_rejected_blacklist"                -> "No pudimos procesar tu pago.";
            case "cc_rejected_call_for_authorize"       -> "Debes autorizar el pago con tu banco.";
            case "cc_rejected_card_disabled"            -> "Tu tarjeta esta inactiva. Activala con tu banco.";
            case "cc_rejected_card_error"               -> "No pudimos procesar tu tarjeta.";
            case "cc_rejected_duplicated_payment"       -> "Ya realizaste un pago con ese importe. Usa otra tarjeta.";
            case "cc_rejected_high_risk"                -> "Tu pago fue rechazado. Elige otro medio de pago.";
            case "cc_rejected_insufficient_amount"      -> "Fondos insuficientes en tu tarjeta.";
            case "cc_rejected_invalid_installments"     -> "Las cuotas seleccionadas no estan disponibles.";
            case "cc_rejected_max_attempts"             -> "Alcanzaste el limite de intentos. Usa otra tarjeta.";
            default -> "Tu pago no pudo ser procesado (" + statusDetail + "). Intenta con otro medio de pago.";
        };
    }
}
