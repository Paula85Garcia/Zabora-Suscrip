package com.zabora.subscription.controlador;

import com.zabora.subscription.modelo.dto.RespuestaPagoDTO;
import com.zabora.subscription.modelo.dto.SolicitudPagoDTO;
import com.zabora.subscription.servicio.PagoServicioReal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestion de pagos
 * Maneja creacion de Payment Intents y consultas de estado
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Endpoints para gestion de pagos con Stripe")
public class PagoControlador {
    
    private final PagoServicioReal pagoServicio;
    
    /**
     * Crear Payment Intent en Stripe
     * @param authentication Usuario autenticado
     * @param solicitud Datos del pago a crear
     * @return Respuesta con client_secret para Stripe Elements
     */
    @PostMapping("/crear-intent")
    @Operation(summary = "Crear Payment Intent en Stripe")
    public ResponseEntity<RespuestaPagoDTO> crearPaymentIntent(
            Authentication authentication,
            @Valid @RequestBody SolicitudPagoDTO solicitud) {
        
        String usuarioId = authentication.getName();
        RespuestaPagoDTO resultado = pagoServicio.crearIntentoPago(usuarioId, solicitud);
        return ResponseEntity.ok(resultado);
    }
    
    /**
     * Consultar estado de un pago especifico
     * @param paymentIntentId ID del Payment Intent de Stripe
     * @return Estado del pago
     */
    @GetMapping("/estado/{paymentIntentId}")
    @Operation(summary = "Consultar estado de un pago")
    public ResponseEntity<Map<String, Object>> consultarEstadoPago(
            @PathVariable String paymentIntentId) {
        
        Map<String, Object> estado = pagoServicio.obtenerEstadoPago(paymentIntentId);
        return ResponseEntity.ok(estado);
    }
    
    /**
     * Obtener historial completo de pagos del usuario
     * @param authentication Usuario autenticado
     * @return Lista de pagos realizados
     */
    @GetMapping("/historial")
    @Operation(summary = "Obtener historial de pagos del usuario")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorial(
            Authentication authentication) {
        
        String usuarioId = authentication.getName();
        List<Map<String, Object>> historial = pagoServicio.obtenerHistorialPagos(usuarioId);
        return ResponseEntity.ok(historial);
    }
}