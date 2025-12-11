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

/**
 * Controlador para la gestión de suscripciones de usuarios.
 * Permite suscribirse a planes, cancelar, verificar estado y obtener datos mock para pruebas.
 */
@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@Tag(name = "Suscripciones", description = "Endpoints para gestión de suscripciones")
public class SuscripcionControlador {

    private final SuscripcionServicio suscripcionServicio;

    /**
     * Suscribirse a un plan determinado.
     *
     * @param usuarioId ID del usuario que realiza la suscripción (header X-Usuario-Id)
     * @param solicitud Datos de la suscripción, incluyendo nombre del plan y método de pago
     * @return Respuesta con estado de la suscripción, plan, límites, y si requiere pago
     */
    @PostMapping("/suscribir")
    @Operation(summary = "Suscribirse a un plan")
    public ResponseEntity<RespuestaSuscripcionDTO> suscribirse(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @Valid @RequestBody SolicitudSuscripcionDTO solicitud) {
        RespuestaSuscripcionDTO respuesta = suscripcionServicio.suscribirse(usuarioId, solicitud);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Cancela una suscripción activa de un usuario.
     *
     * @param usuarioId ID del usuario (header X-Usuario-Id)
     * @param idSuscripcion ID de la suscripción a cancelar
     * @return Respuesta con estado actualizado y fecha de cancelación
     */
    @PostMapping("/cancelar/{idSuscripcion}")
    @Operation(summary = "Cancelar suscripción")
    public ResponseEntity<RespuestaSuscripcionDTO> cancelarSuscripcion(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @PathVariable String idSuscripcion) {
        RespuestaSuscripcionDTO respuesta = suscripcionServicio.cancelarSuscripcion(usuarioId, idSuscripcion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Obtiene el estado actual de la suscripción de un usuario.
     *
     * @param usuarioId ID del usuario (header X-Usuario-Id)
     * @return Estado de la suscripción, plan activo y límites del plan
     */
    @GetMapping("/estado")
    @Operation(summary = "Obtener estado de suscripción")
    public ResponseEntity<Map<String, Object>> obtenerEstado(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        Map<String, Object> estado = suscripcionServicio.obtenerEstadoSuscripcion(usuarioId);
        return ResponseEntity.ok(estado);
    }

    /**
     * Verifica la suscripción de un usuario (uso interno).
     *
     * @param usuarioId ID del usuario a verificar
     * @return Resultado de la verificación indicando validez, plan, estado y fecha de expiración
     */
    @GetMapping("/verificar/{usuarioId}")
    @Operation(summary = "Verificar suscripción (uso interno)")
    public ResponseEntity<RespuestaVerificacionDTO> verificarSuscripcion(
            @PathVariable String usuarioId) {
        RespuestaVerificacionDTO verificacion = suscripcionServicio.verificarSuscripcion(usuarioId);
        return ResponseEntity.ok(verificacion);
    }

    /**
     * Obtiene datos mock para pruebas de suscripción.
     *
     * @return Mapa con datos simulados de suscripciones y planes
     */
   /* @GetMapping("/datos-mock")
    @Operation(summary = "Obtener datos mock para pruebas")
    public ResponseEntity<Map<String, Object>> obtenerDatosMock() {
        Map<String, Object> datos = suscripcionServicio.obtenerDatosMock();
        return ResponseEntity.ok(datos);
    }*/

    /**
     * Obtiene todos los planes de suscripción disponibles en el sistema.
     *
     * @return Lista de planes con descripción, precio y límites
     */
    @GetMapping("/planes")
    @Operation(summary = "Obtener todos los planes disponibles")
    public ResponseEntity<?> obtenerPlanes() {
        return ResponseEntity.ok(suscripcionServicio.obtenerPlanes());
    }
}
