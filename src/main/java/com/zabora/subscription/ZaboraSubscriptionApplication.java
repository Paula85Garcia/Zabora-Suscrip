package com.zabora.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
@EnableScheduling
public class ZaboraSubscriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZaboraSubscriptionApplication.class, args);

        System.out.println("===============================================");
        System.out.println("Zabora Subscription Service iniciado!");
        System.out.println("Registrando en Consul en: http://localhost:8500");
        System.out.println("===============================================");
        System.out.println("URLs disponibles:");
        System.out.println("  Swagger UI: http://localhost:8081/swagger-ui.html");  
        System.out.println("  API Docs:   http://localhost:8081/api-docs");         
        System.out.println("  Health:     http://localhost:8081/actuator/health");  
        System.out.println();
        System.out.println("Endpoints principales:");
        System.out.println("  POST   /api/suscripciones/suscribir");
        System.out.println("  POST   /api/suscripciones/cancelar/{id}");
        System.out.println("  GET    /api/suscripciones/estado");
        System.out.println("  GET    /api/suscripciones/planes");
        System.out.println("  POST   /api/pagos/crear-preferencia");
        System.out.println("  POST   /api/webhooks/mercadopago");
        System.out.println("===============================================");
    }

   
}