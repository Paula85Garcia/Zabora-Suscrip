package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.modelo.dto.CrearPagoRequest;
import com.zabora.subscription.modelo.dto.CrearPagoResponse;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import com.zabora.subscription.servicio.MercadoPagoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final MercadoPagoServicio mercadoPagoServicio;
    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;

    @PostMapping("/crear-preferencia")
    public ResponseEntity<?> crearPreferenciaPago(@Valid @RequestBody CrearPagoRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Creando preferencia de pago MercadoPago");
            log.info("Suscripcion ID: {}", request.getIdSuscripcion());
            log.info("Monto: {} COP", request.getMonto());

             // 1. Obtener usuario
             Integer usuarioId = null;
                try {
                usuarioId = UserContext.get().getUserId();
                log.info("Usuario ID desde contexto: {}", usuarioId);
            } catch (Exception e) {
                log.error("Error obteniendo UserContext: {}", e.getMessage());
                response.put("success", false);
                response.put("error", "Error de autenticación: No se pudo identificar al usuario");
                return ResponseEntity.status(401).body(response);
            }

            if (usuarioId == null) {
                log.error("Usuario ID es null");
                response.put("success", false);
                response.put("error", "Usuario no autenticado");
                return ResponseEntity.status(401).body(response);
            }
            
            // 2. Buscar la suscripción
            UsuarioSuscripcion suscripcion = suscripcionRepositorio
                    .findById(request.getIdSuscripcion())
                    .orElseThrow(() -> new RuntimeException("Suscripcion no encontrada"));

            log.info("Suscripción encontrada:");
            log.info("  - ID: {}", suscripcion.getId());
            log.info("  - Usuario ID en BD: {}", suscripcion.getUsuarioId());
            log.info("  - Estado: {}", suscripcion.getEstado());
            
            // 3. Verificar que la suscripción pertenezca al usuario
            if (!suscripcion.getUsuarioId().equals(usuarioId)) {
                log.error("La suscripción no pertenece al usuario. Dueño real: {}", suscripcion.getUsuarioId());
                response.put("success", false);
                response.put("error", "La suscripción no pertenece al usuario");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. Verificar que no tenga otro pago pendiente
            boolean existePagoPendiente = pagoRepositorio
                    .existsBySuscripcionIdAndEstado(request.getIdSuscripcion(), EstadoPago.PENDIENTE);

            if (existePagoPendiente) {
                log.warn("Ya existe un pago pendiente para esta suscripción");
                response.put("success", false);
                response.put("error", "Ya existe un pago pendiente para esta suscripcion");
             return ResponseEntity.badRequest().body(response);
            }

            // 5. Crear preferencia en MercadoPago
            CrearPagoResponse mpResponse = mercadoPagoServicio.crearPreferenciaPago(request);

            // 6. Guardar el pago en BD
            Pago pago = new Pago();
            pago.setId(UUID.randomUUID().toString());
            pago.setSuscripcionId(request.getIdSuscripcion());
            pago.setUsuarioId(suscripcion.getUsuarioId());
            pago.setMonto(request.getMonto());
            pago.setMoneda("COP");
            pago.setMetodoPago(mapearTipoPago(request.getTipoPago()));
            pago.setEstado(EstadoPago.PENDIENTE);
            pago.setIdIntentoPago(mpResponse.getPreferenceId());

            pagoRepositorio.save(pago);
            log.info("Pago guardado en BD con ID: {}", pago.getId());

             // 7. Completar la respuesta
            mpResponse.setSubscriptionId(request.getIdSuscripcion());
            mpResponse.setPaymentId(pago.getId());

            log.info("PREFERENCIA CREADA EXITOSAMENTE");
            log.info("Preference ID: {}", mpResponse.getPreferenceId());
            log.info("Init Point: {}", mpResponse.getInitPoint());

            return ResponseEntity.ok(mpResponse);

        } catch (Exception e) {
            log.error("Error creando preferencia de pago: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> obtenerPublicKey() {
        Map<String, String> response = new HashMap<>();

        response.put("publicKey", mercadoPagoServicio.getPublicKey());
        return ResponseEntity.ok(response);
    }

    private String mapearTipoPago(String tipoPago) {
        if ("tarjeta_credito".equalsIgnoreCase(tipoPago)) {
            return "TARJETA_CREDITO";
        } else if ("pse".equalsIgnoreCase(tipoPago)) {
            return "PSE";
        }
        return "TARJETA_CREDITO";
    }
}