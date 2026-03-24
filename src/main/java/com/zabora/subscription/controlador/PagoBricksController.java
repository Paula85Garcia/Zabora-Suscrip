package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.modelo.dto.CrearPagoBricksRequest;
import com.zabora.subscription.modelo.dto.CrearPagoBricksResponse;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import com.zabora.subscription.servicio.MercadoPagoBricksServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador para MercadoPago Checkout Bricks
 * Permite pagos embebidos sin redirección externa
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos/bricks")
@RequiredArgsConstructor
public class PagoBricksController {

    private final MercadoPagoBricksServicio mercadoPagoBricksServicio;
    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    /**
     * Obtener Public Key de MercadoPago (endpoint público)
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        log.info("🔑 Obteniendo public key de MercadoPago");
        
        Map<String, String> response = new HashMap<>();
        response.put("publicKey", publicKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Crear preferencia de pago para Bricks
     */
    @PostMapping("/preference")
    public ResponseEntity<?> createPreference(@Valid @RequestBody CrearPagoBricksRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("═══════════════════════════════════════");
            log.info("📝 CREANDO PREFERENCIA PARA BRICKS");
            log.info("═══════════════════════════════════════");
            
            // 1. Obtener usuario
            Integer usuarioId = UserContext.get().getUserId();
            String email = UserContext.get().getEmail();
            
            log.info("👤 Usuario ID: {}", usuarioId);
            log.info("📧 Email: {}", email);
            log.info("💳 Tipo de pago: {}", request.getTipoPago());
            log.info("💰 Monto: {} COP", request.getMonto());

            // 2. Buscar la suscripción
            UsuarioSuscripcion suscripcion = suscripcionRepositorio
                    .findById(request.getIdSuscripcion())
                    .orElseThrow(() -> new RuntimeException("Suscripción no encontrada"));

            log.info("📋 Suscripción encontrada: {}", suscripcion.getId());
            log.info("   - Estado: {}", suscripcion.getEstado());
            log.info("   - Plan: {}", suscripcion.getPlan().getNombre());
            
            // 3. Verificar que la suscripción pertenezca al usuario
            if (!suscripcion.getUsuarioId().equals(usuarioId)) {
                log.error("❌ La suscripción no pertenece al usuario");
                response.put("success", false);
                response.put("error", "La suscripción no pertenece al usuario");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. Verificar que no tenga otro pago pendiente
            boolean existePagoPendiente = pagoRepositorio
                    .existsBySuscripcionIdAndEstado(request.getIdSuscripcion(), EstadoPago.PENDIENTE);

            if (existePagoPendiente) {
                log.warn("⚠️ Ya existe un pago pendiente");
                response.put("success", false);
                response.put("error", "Ya existe un pago pendiente para esta suscripción");
                return ResponseEntity.badRequest().body(response);
            }

            // 5. Crear preferencia para Bricks (usando el servicio correcto)
            var bricksResponse = mercadoPagoBricksServicio.crearPreferenciaBricks(request, usuarioId);

            // 6. El servicio ya guardó el pago en BD, solo retornamos la respuesta
            log.info("💾 Pago guardado en BD: {}", bricksResponse.getPaymentId());

            // 7. Retornar respuesta para Bricks (SIN initPoint - pago embebido, no redirect)
            CrearPagoBricksResponse responseBricks = CrearPagoBricksResponse.builder()
                    .preferenceId(bricksResponse.getPreferenceId())
                    .initPoint(null)  // Importante: SIN initPoint para Bricks
                    .sandboxInitPoint(null)  // Importante: SIN sandboxInitPoint para Bricks
                    .publicKey(publicKey)
                    .amount(request.getMonto())
                    .currency("COP")
                    .subscriptionId(request.getIdSuscripcion())
                    .paymentId(bricksResponse.getPaymentId())
                    .build();

            log.info("✅ PREFERENCIA CREADA EXITOSAMENTE");
            log.info("   Preference ID: {}", bricksResponse.getPreferenceId());
            log.info("═══════════════════════════════════════");

            return ResponseEntity.ok(responseBricks);

        } catch (Exception e) {
            log.error("❌ Error creando preferencia: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Verificar estado de pago PSE (público para polling)
     */
    @GetMapping("/pse/verification")
    public ResponseEntity<?> verifyPSEPayment(@RequestParam String paymentId) {
        log.info("🔍 Verificando estado de pago PSE: {}", paymentId);
        
        try {
            Pago pago = pagoRepositorio.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", pago.getId());
            response.put("estado", pago.getEstado().name());
            response.put("monto", pago.getMonto());
            response.put("fechaPago", pago.getFechaPago());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error verificando pago: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private String mapearTipoPago(String tipoPago) {
        return switch (tipoPago.toLowerCase()) {
            case "tarjeta_credito", "card" -> "TARJETA_CREDITO";
            case "pse" -> "PSE";
            default -> "TARJETA_CREDITO";
        };
    }

    @PostMapping("/process")
    public ResponseEntity<?> procesarPago(@RequestBody Map<String, Object> paymentData) {
        try {
            Integer usuarioId = UserContext.get().getUserId();
            log.info("Procesando pago para usuario: {}", usuarioId);
            log.info("Payment data recibido: {}", paymentData);

            // Determinar tipo de pago
            String paymentType = (String) paymentData.get("paymentType");
            String selectedPaymentMethod = (String) paymentData.get("selectedPaymentMethod");
            String suscripcionId = (String) paymentData.get("suscripcion_id");
            
            log.info("Tipo de pago: {}, Método: {}", paymentType, selectedPaymentMethod);

            // Buscar el pago pendiente
            Pago pagoPendiente = pagoRepositorio
                .findBySuscripcionIdAndEstado(suscripcionId, EstadoPago.PENDIENTE)
                .orElseThrow(() -> new RuntimeException("Pago pendiente no encontrado"));

            Map<String, Object> resultado;
            
            if ("bank_transfer".equals(paymentType) || "pse".equals(selectedPaymentMethod)) {
                // Procesar PSE (sin token)
                resultado = mercadoPagoBricksServicio.procesarPagoPSE(
                    paymentData, pagoPendiente, suscripcionId);
            } else {
                // Procesar tarjeta (con token)
                Map<String, Object> formData = (Map<String, Object>) paymentData.get("formData");
                String token = (String) formData.get("token");
                String paymentMethodId = (String) formData.get("payment_method_id");
                String issuerId = String.valueOf(formData.get("issuer_id"));
                Integer installments = (Integer) formData.getOrDefault("installments", 1);
                String email = (String) ((Map<?, ?>) formData.getOrDefault(
                    "payer", Map.of("email", ""))).get("email");
                
                resultado = mercadoPagoBricksServicio
                    .procesarPagoConToken(token, paymentMethodId, issuerId, 
                                           installments, email, pagoPendiente, suscripcionId);
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error procesando pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}