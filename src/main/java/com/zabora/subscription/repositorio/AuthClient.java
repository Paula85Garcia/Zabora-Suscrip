package com.zabora.subscription.repositorio;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PutMapping("/api/upgrade/premium/{userId}")
    void actualizarRolPremium(
            @PathVariable String userId
//            @RequestParam String rol
    );
}
