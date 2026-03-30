package com.zabora.subscription.controlador;

import com.zabora.subscription.repositorio.PlanSuscripcionRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@Tag(name = "Planes", description = "Consulta de planes disponibles")
public class PlanesController {

    private final PlanSuscripcionRepositorio planRepositorio;

    @GetMapping("/planes")
    @Operation(summary = "Listar planes disponibles")
    public ResponseEntity<?> obtenerPlanes() {
        log.info("Consultando planes");
        try {
            return ResponseEntity.ok(planRepositorio.findByActivoTrue());
        } catch (Exception e) {
            log.error("Error consultando planes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al consultar planes"));
        }
    }
}
