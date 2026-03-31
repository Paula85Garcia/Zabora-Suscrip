package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.repositorio.PagoRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Historial de pagos del usuario")
public class PagosController {

    private final PagoRepositorio pagoRepositorio;

    @GetMapping("/mis-pagos")
    @Operation(summary = "Obtener historial de pagos del usuario autenticado")
    public ResponseEntity<?> misPagos() {
        var userData = UserContext.get();
        if (userData == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
        }
        log.info("Consultando pagos - Usuario: {}", userData.getUserId());
        try {
            return ResponseEntity.ok(
                pagoRepositorio.findByUsuarioIdOrderByFechaCreacionDesc(userData.getUserId())
            );
        } catch (Exception e) {
            log.error("Error consultando pagos: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al consultar pagos"));
        }
    }
}