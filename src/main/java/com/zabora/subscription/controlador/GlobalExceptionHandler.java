package com.zabora.subscription.controlador;

import com.zabora.subscription.excepcion.AuthServiceException;
import com.zabora.subscription.excepcion.PagoRechazadoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejo centralizado de excepciones del servicio de suscripciones.
 * Reemplaza el GlobalExceptionHandler anterior añadiendo los nuevos tipos de dominio.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validacion fallida: {}", errores);
        return ResponseEntity.badRequest().body(Map.of(
            "error",   "Datos invalidos",
            "detalle", errores,
            "status",  HttpStatus.BAD_REQUEST.value()
        ));
    }

    @ExceptionHandler(PagoRechazadoException.class)
    public ResponseEntity<Map<String, Object>> handlePagoRechazado(PagoRechazadoException ex) {
        log.warn("Pago rechazado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
            "error",        "Pago rechazado",
            "statusDetail", ex.getStatusDetail(),
            "mpPaymentId",  ex.getMpPaymentId(),
            "status",       HttpStatus.UNPROCESSABLE_ENTITY.value()
        ));
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAuthService(AuthServiceException ex) {
        // El auth-service fallo DESPUES de que la suscripcion ya se activo en BD.
        // Respondemos 200 para no alarmar al usuario — la suscripcion esta activa.
        // El rol se corregira manualmente o en el siguiente intento de login.
        log.error("Auth-service error (no critico para el usuario): {}", ex.getMessage());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "warning", "Suscripcion activa. Rol pendiente de sincronizacion con el servidor de autenticacion.",
            "status",  HttpStatus.OK.value()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado invalido: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
            "error",  ex.getMessage(),
            "status", HttpStatus.BAD_REQUEST.value()
        ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error",  ex.getMessage(),
            "status", HttpStatus.FORBIDDEN.value()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Error interno: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error",  "Error interno del servidor",
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }
}
