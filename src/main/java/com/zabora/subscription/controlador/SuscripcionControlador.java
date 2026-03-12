package com.zabora.subscription.controlador;

import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.RespuestaVerificacionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.servicio.SuscripcionServicioReal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.zabora.subscription.data.UserContext;
import java.util.Map;

/**
 * Controlador REST para gestion de suscripciones Maneja creacion, cancelacion y
 * consulta de suscripciones
 */
@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@Tag(name = "Suscripciones", description = "Endpoints para gestion de suscripciones")
public class SuscripcionControlador {

    private final SuscripcionServicioReal suscripcionServicio;

    /**
     * Suscribir usuario a un plan
     *
     * @param authentication Usuario autenticado
     * @param solicitud Datos de la suscripcion
     * @return Respuesta con detalles de la suscripcion creada
     */
    @PostMapping("/suscribir")
    @Operation(summary = "Suscribirse a un plan")
    public ResponseEntity<RespuestaSuscripcionDTO> suscribirse(
            // Authentication authentication,
            @Valid @RequestBody SolicitudSuscripcionDTO solicitud) {

        Integer usuarioId = UserContext.get().getUserId();
        RespuestaSuscripcionDTO respuesta = suscripcionServicio.suscribirse(usuarioId, solicitud);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Cancelar suscripcion activa
     *
     * @param authentication Usuario autenticado
     * @param idSuscripcion ID de la suscripcion a cancelar
     * @param inmediata Si es true, cancela inmediatamente. Si es false, al
     * final del periodo
     * @return Respuesta con detalles de la cancelacion
     */
    @PostMapping("/cancelar/{idSuscripcion}")
    @Operation(summary = "Cancelar suscripcion")
    public ResponseEntity<RespuestaSuscripcionDTO> cancelarSuscripcion(
            // Authentication authentication,
            @PathVariable String idSuscripcion,
            @RequestParam(defaultValue = "false") Boolean inmediata) {

        Integer usuarioId = UserContext.get().getUserId();
        RespuestaSuscripcionDTO respuesta = suscripcionServicio.cancelarSuscripcion(
                usuarioId, idSuscripcion, inmediata);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Obtener estado completo de la suscripcion del usuario
     *
     * @param authentication Usuario autenticado
     * @return Estado detallado de la suscripcion
     */
    @GetMapping("/estado")
    @Operation(summary = "Obtener estado de suscripcion del usuario autenticado")
    public ResponseEntity<Map<String, Object>> obtenerEstado( // Authentication authentication
            ) {

        Integer usuarioId = UserContext.get().getUserId();
        Map<String, Object> estado = suscripcionServicio.obtenerEstadoSuscripcion(usuarioId);
        return ResponseEntity.ok(estado);
    }

    /**
     * Verificar suscripcion de un usuario (para otros microservicios)
     *
     * @param usuarioId ID del usuario a verificar
     * @return Verificacion de suscripcion premium
     */
    @GetMapping("/verificar/{usuarioId}")
    @Operation(summary = "Verificar suscripcion (uso interno)")
    public ResponseEntity<RespuestaVerificacionDTO> verificarSuscripcion(
            @PathVariable Integer usuarioId) {

        RespuestaVerificacionDTO verificacion = suscripcionServicio.verificarSuscripcion(usuarioId);
        return ResponseEntity.ok(verificacion);
    }

    /**
     * Obtener todos los planes disponibles (endpoint publico)
     *
     * @return Lista de planes
     */
    @GetMapping("/planes")
    @Operation(summary = "Obtener todos los planes disponibles")
    public ResponseEntity<?> obtenerPlanes() {
        return ResponseEntity.ok(suscripcionServicio.obtenerPlanes());
    }
}
