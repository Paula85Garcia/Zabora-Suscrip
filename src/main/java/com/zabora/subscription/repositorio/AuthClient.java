package com.zabora.subscription.repositorio;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "auth-service", url = "http://localhost:8000")
public interface AuthClient {

   /**
     * Actualizar usuario a PREMIUM
     * Endpoint: POST /api/upgrade/premium/{userId}
     */
    @PostMapping("/api/upgrade/premium/{userId}")
    void actualizarRolPremium(@PathVariable("userId") Integer userId);

    /**
     * Revertir usuario a GRATUITO
     * Endpoint: POST /api/upgrade/downgrade/{userId}
     */
    @PostMapping("/api/upgrade/downgrade/{userId}")
    void revertirAGratuito(@PathVariable("userId") Integer userId);
}