package com.zabora.subscription.controlador;

import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.RespuestaVerificacionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.servicio.SuscripcionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@Tag(name = "Suscripciones", description = "Endpoints para gestión de suscripciones")
public class SuscripcionControlador {
    
    private final SuscripcionServicio suscripcionServicio;
    
    @PostMapping("/suscribir")
    @Operation(summary = "Suscribirse a un plan")
    public ResponseEntity<RespuestaSuscripcionDTO> suscribirse(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @Valid @RequestBody SolicitudSuscripcionDTO solicitud) {
        RespuestaSuscripcionDTO respuesta = suscripcionServicio.suscribirse(usuarioId, solicitud);
        return ResponseEntity.ok(respuesta);
    }
    
    @PostMapping("/cancelar/{idSuscripcion}")
    @Operation(summary = "Cancelar suscripción")
    public ResponseEntity<RespuestaSuscripcionDTO> cancelarSuscripcion(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @PathVariable String idSuscripcion) {
        RespuestaSuscripcionDTO respuesta = suscripcionServicio.cancelarSuscripcion(usuarioId, idSuscripcion);
        return ResponseEntity.ok(respuesta);
    }
    
    @GetMapping("/estado")
    @Operation(summary = "Obtener estado de suscripción")
    public ResponseEntity<Map<String, Object>> obtenerEstado(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        Map<String, Object> estado = suscripcionServicio.obtenerEstadoSuscripcion(usuarioId);
        return ResponseEntity.ok(estado);
    }
    
    @GetMapping("/verificar/{usuarioId}")
    @Operation(summary = "Verificar suscripción (uso interno)")
    public ResponseEntity<RespuestaVerificacionDTO> verificarSuscripcion(
            @PathVariable String usuarioId) {
        RespuestaVerificacionDTO verificacion = suscripcionServicio.verificarSuscripcion(usuarioId);
        return ResponseEntity.ok(verificacion);
    }
    
    @GetMapping("/datos-mock")
    @Operation(summary = "Obtener datos mock para pruebas")
    public ResponseEntity<Map<String, Object>> obtenerDatosMock() {
        Map<String, Object> datos = suscripcionServicio.obtenerDatosMock();
        return ResponseEntity.ok(datos);
    }
    
    @GetMapping("/planes")
    @Operation(summary = "Obtener todos los planes disponibles")
    public ResponseEntity<?> obtenerPlanes() {
        return ResponseEntity.ok(suscripcionServicio.obtenerPlanes());
    }
}