package com.zabora.subscription.excepcion;

import com.zabora.subscription.modelo.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SuscripcionException.class)
    public ResponseEntity<ErrorDTO> handleSuscripcionException(
            SuscripcionException ex, WebRequest request) {
        
        log.error("Error de suscripción: {}", ex.getMessage());
        
        ErrorDTO error = ErrorDTO.builder()
            .timestamp(LocalDateTime.now())
            .mensaje(ex.getMessage())
            .detalle("Error al procesar la suscripción")
            .ruta(request.getDescription(false).replace("uri=", ""))
            .codigoError("SUSCRIPCION_ERROR")
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(PagoException.class)
    public ResponseEntity<ErrorDTO> handlePagoException(
            PagoException ex, WebRequest request) {
        
        log.error("Error de pago: {}", ex.getMessage());
        
        ErrorDTO error = ErrorDTO.builder()
            .timestamp(LocalDateTime.now())
            .mensaje(ex.getMessage())
            .detalle("Error al procesar el pago")
            .ruta(request.getDescription(false).replace("uri=", ""))
            .codigoError("PAGO_ERROR")
            .build();
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }
    
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorDTO> handleRecursoNoEncontrado(
            RecursoNoEncontradoException ex, WebRequest request) {
        
        log.error("Recurso no encontrado: {}", ex.getMessage());
        
        ErrorDTO error = ErrorDTO.builder()
            .timestamp(LocalDateTime.now())
            .mensaje(ex.getMessage())
            .detalle("El recurso solicitado no existe")
            .ruta(request.getDescription(false).replace("uri=", ""))
            .codigoError("RECURSO_NO_ENCONTRADO")
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidacionException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String errores = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        log.error("Error de validación: {}", errores);
        
        ErrorDTO error = ErrorDTO.builder()
            .timestamp(LocalDateTime.now())
            .mensaje("Errores de validación en los datos enviados")
            .detalle(errores)
            .ruta(request.getDescription(false).replace("uri=", ""))
            .codigoError("VALIDACION_ERROR")
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleExcepcionGeneral(
            Exception ex, WebRequest request) {
        
        log.error("Error interno del servidor: ", ex);
        
        ErrorDTO error = ErrorDTO.builder()
            .timestamp(LocalDateTime.now())
            .mensaje("Error interno del servidor")
            .detalle(ex.getMessage())
            .ruta(request.getDescription(false).replace("uri=", ""))
            .codigoError("ERROR_INTERNO")
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}