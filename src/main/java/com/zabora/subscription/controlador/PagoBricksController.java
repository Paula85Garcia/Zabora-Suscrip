package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.modelo.dto.CrearPagoBricksRequest;
import com.zabora.subscription.modelo.dto.CrearPagoBricksResponse;
import com.zabora.subscription.servicio.PagoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/api/pagos/bricks")
@RequiredArgsConstructor
@Tag(name = "Pagos - Bricks", description = "Pagos con MercadoPago Checkout Bricks")
public class PagoBricksController {

    private static final Logger log = LoggerFactory.getLogger(PagoBricksController.class);

    private final PagoServicio pagoServicio;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @GetMapping("/public-key")
    @Operation(summary = "Obtener public key de MercadoPago")
    public ResponseEntity<Map<String, String>> obtenerPublicKey() {
        log.info("Solicitud de public key");
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @PostMapping("/preference")
    @Operation(summary = "Crear preferencia de pago para Bricks")
    public ResponseEntity<?> crearPreferencia(@Valid @RequestBody CrearPagoBricksRequest request) {
        log.info("Creando preferencia - Suscripcion: {}", request.getIdSuscripcion());

        Integer usuarioId = obtenerUsuarioId();
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            CrearPagoBricksResponse response = pagoServicio.crearPreferencia(request, usuarioId);
            log.info("Preferencia creada: {}", response.getPreferenceId());
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Acceso denegado: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Estado invalido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al crear preferencia: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear preferencia de pago"));
        }
    }

    @PostMapping("/process")
    @Operation(summary = "Verificar resultado del pago de Bricks")
    public ResponseEntity<?> procesarPago(@RequestBody Map<String, Object> paymentData) {
        log.info("Verificando pago de Bricks");

        Integer usuarioId = obtenerUsuarioId();
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            Map<String, Object> resultado = pagoServicio.procesarPago(paymentData, usuarioId);
            return ResponseEntity.ok(resultado);
        } catch (IllegalStateException e) {
            log.warn("Error procesando pago: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error procesando pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al verificar el pago"));
        }
    }

    private Integer obtenerUsuarioId() {
        var userData = UserContext.get();
        return (userData != null) ? userData.getUserId() : null;
    }
}