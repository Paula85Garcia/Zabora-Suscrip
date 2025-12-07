package com.zabora.subscription.controlador;

import com.zabora.subscription.modelo.dto.RespuestaPagoDTO;
import com.zabora.subscription.modelo.dto.SolicitudPagoDTO;
import com.zabora.subscription.servicio.PagoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Endpoints para gestión de pagos")
public class PagoControlador {
    
    private final PagoServicio pagoServicio;
    
    @PostMapping("/procesar")
    @Operation(summary = "Procesar pago manual")
    public ResponseEntity<RespuestaPagoDTO> procesarPago(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @Valid @RequestBody SolicitudPagoDTO solicitud) {
        RespuestaPagoDTO resultado = pagoServicio.procesarPago(solicitud);
        return ResponseEntity.ok(resultado);
    }
    
    @GetMapping("/metodos")
    @Operation(summary = "Obtener métodos de pago del usuario")
    public ResponseEntity<List<Map<String, Object>>> obtenerMetodosPago(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        List<Map<String, Object>> metodos = pagoServicio.obtenerMetodosPago(usuarioId);
        return ResponseEntity.ok(metodos);
    }
    
    @PostMapping("/metodos")
    @Operation(summary = "Agregar método de pago")
    public ResponseEntity<Map<String, Object>> agregarMetodoPago(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestBody Map<String, Object> datosMetodo) {
        Map<String, Object> resultado = pagoServicio.agregarMetodoPago(usuarioId, datosMetodo);
        return ResponseEntity.ok(resultado);
    }
    
    @DeleteMapping("/metodos/{idMetodo}")
    @Operation(summary = "Eliminar método de pago")
    public ResponseEntity<Map<String, Object>> eliminarMetodoPago(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @PathVariable String idMetodo) {
        Map<String, Object> resultado = pagoServicio.eliminarMetodoPago(usuarioId, idMetodo);
        return ResponseEntity.ok(resultado);
    }
    
    @PostMapping("/pago-prueba")
    @Operation(summary = "Procesar pago de prueba con diferentes escenarios")
    public ResponseEntity<RespuestaPagoDTO> procesarPagoPrueba(
            @RequestBody Map<String, Object> solicitudPrueba) {
        SolicitudPagoDTO solicitud = new SolicitudPagoDTO();
        solicitud.setIdSuscripcion((String) solicitudPrueba.get("id_suscripcion"));
        solicitud.setMonto(new java.math.BigDecimal(solicitudPrueba.get("monto").toString()));
        solicitud.setTipoPago((String) solicitudPrueba.get("tipo_pago"));
        solicitud.setTokenTarjetaPrueba((String) solicitudPrueba.get("token_prueba"));
        
        RespuestaPagoDTO resultado = pagoServicio.procesarPago(solicitud);
        return ResponseEntity.ok(resultado);
    }
}
