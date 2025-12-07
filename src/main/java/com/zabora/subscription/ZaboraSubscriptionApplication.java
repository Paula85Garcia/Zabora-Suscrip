package com.zabora.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZaboraSubscriptionApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ZaboraSubscriptionApplication.class, args);
        
        System.out.println("Zabora Subscription Service iniciado!");
        System.out.println("Swagger UI disponible en: http://localhost:8080/swagger-ui.html");
        System.out.println("Endpoints disponibles:");
        System.out.println("  POST   /api/suscripciones/suscribir");
        System.out.println("  POST   /api/suscripciones/cancelar/{id}");
        System.out.println("  GET    /api/suscripciones/estado");
        System.out.println("  GET    /api/suscripciones/verificar/{usuarioId}");
        System.out.println("  POST   /api/pagos/procesar");
        System.out.println("  GET    /api/pagos/metodos");
        System.out.println("Datos mock disponibles en: /api/suscripciones/datos-mock");
    }
}