package com.zabora.subscription.repositorio;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cliente Feign para comunicarse con el auth-service.
 *
 * El auth-service expone en /api/upgrade:
 *   POST /premium/{userId}   -> sube el usuario a PREMIUM
 *   POST /downgrade/{userId} -> baja el usuario a GRATUITO
 *
 * Si Consul esta activo y el auth-service esta registrado como "auth-service",
 * Feign resuelve la URL automaticamente por nombre.
 *
 * Si NO usas Consul, cambia la anotacion a:
 *   @FeignClient(name = "auth-service", url = "http://localhost:8000")
 */
@FeignClient(name = "auth-service")
public interface AuthClient {

    /**
     * Sube el usuario a PREMIUM.
     * Llama a POST /api/upgrade/premium/{userId} en el auth-service.
     */
    @PostMapping("/api/upgrade/premium/{userId}")
    void actualizarRolPremium(@PathVariable("userId") Integer userId);

    /**
     * Baja el usuario a GRATUITO.
     * Llama a POST /api/upgrade/downgrade/{userId} en el auth-service.
     */
    @PostMapping("/api/upgrade/downgrade/{userId}")
    void revertirAGratuito(@PathVariable("userId") Integer userId);
}