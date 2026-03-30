package com.zabora.subscription.repositorio;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Cliente Feign para comunicarse con el auth-service.
 *
 * El auth-service expone en /api/upgrade:
 *   POST /premium/{userId}   -> sube el usuario a PREMIUM
 *   POST /downgrade/{userId} -> baja el usuario a GRATUITO
 *
 * Consul resuelve la URL automaticamente por nombre.
 */
@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/api/upgrade/premium/{userId}")
    void actualizarRolPremium(@PathVariable("userId") Integer userId);

    @PostMapping(value = "/api/upgrade/downgrade/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void revertirAGratuito(
            @PathVariable("userId") Integer userId,
            @RequestBody Map<String, String> body);
}
