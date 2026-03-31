package com.zabora.subscription.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializa el SDK de MercadoPago con el access token configurado.
 *
 * El access token se lee de application.yml:
 *   mercadopago.access-token=APP_USR-...
 */
@Slf4j
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("MercadoPago SDK inicializado correctamente");
    }
}
