package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.excepcion.AuthServiceException;
import com.zabora.subscription.servicio.SuscripcionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@Tag(name = "Suscripciones", description = "Gestion de suscripciones de usuario")
public class SuscripcionController {

    private final SuscripcionServicio suscripcionServicio;

    @PostMapping("/suscribir")
    @Operation(summary = "Crear suscripcion")
    public ResponseEntity<?> suscribirse(@Valid @RequestBody SolicitudSuscripcionDTO solicitud) {
        log.info("Solicitud de suscripcion - Plan: {}", solicitud.getNombrePlan());
        try {
            RespuestaSuscripcionDTO respuesta = suscripcionServicio.crearSuscripcion(solicitud);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalStateException e) {
            log.warn("Error al suscribirse: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al crear suscripcion"));
        }
    }

    @GetMapping("/estado")
    @Operation(summary = "Obtener estado de suscripcion")
    public ResponseEntity<?> obtenerEstado() {
        Integer usuarioId = obtenerUsuarioId();
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }
        log.info("Consultando estado - Usuario: {}", usuarioId);
        try {
            return ResponseEntity.ok(suscripcionServicio.obtenerEstado(usuarioId));
        } catch (Exception e) {
            log.error("Error consultando estado: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al consultar estado"));
        }
    }

    @PostMapping("/cancelar/{suscripcionId}")
    @Operation(summary = "Cancelar suscripcion")
    public ResponseEntity<?> cancelar(
            @PathVariable String suscripcionId,
            @RequestParam(defaultValue = "false") boolean inmediata) {

        Integer usuarioId = obtenerUsuarioId();
        if (usuarioId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }
        log.info("Cancelando - ID: {}, Inmediata: {}, Usuario: {}", suscripcionId, inmediata, usuarioId);
        try {
            return ResponseEntity.ok(suscripcionServicio.cancelarSuscripcion(suscripcionId, inmediata, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (AuthServiceException e) {
            log.error("Cancelación bloqueada: auth-service no degradó al usuario: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                "error",
                "No pudimos actualizar tu cuenta en el servicio de usuarios. Reintenta en unos segundos o contacta soporte.",
                "code",
                "AUTH_SYNC_FAILED"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al cancelar: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al cancelar suscripcion"));
        }
    }

    @GetMapping("/verificar/{userId}")
    @Operation(summary = "Verificar suscripcion de un usuario (uso interno)")
    public ResponseEntity<?> verificar(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(suscripcionServicio.obtenerEstado(userId));
        } catch (Exception e) {
            log.error("Error verificando: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Integer obtenerUsuarioId() {
        var userData = UserContext.get();
        return (userData != null) ? userData.getUserId() : null;
    }
}
