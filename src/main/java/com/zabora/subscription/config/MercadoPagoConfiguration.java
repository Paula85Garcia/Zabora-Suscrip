package com.zabora.subscription.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializa el SDK de MercadoPago una sola vez al arrancar.
 * El access token se configura globalmente y todos los clientes
 * (PreferenceClient, PaymentClient) lo usan automaticamente.
 */
@Configuration
public class MercadoPagoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoConfiguration.class);

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("MercadoPago SDK inicializado. Token termina en: ...{}",
            accessToken.substring(Math.max(0, accessToken.length() - 6)));
    }
}