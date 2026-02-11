package com.zabora.subscription.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI zaboraOpenAPI() {
        // Servidor
        Server server = new Server();
        server.setUrl("http://localhost:8081");
        server.setDescription("Servidor de Desarrollo");
        
        // Contacto
        Contact contact = new Contact();
        contact.setEmail("soporte@zabora.com");
        contact.setName("Equipo Zabora");
        contact.setUrl("https://www.zabora.com");
        
        // Licencia
        License mitLicense = new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT");
        
        // Información
        Info info = new Info()
            .title("API de Suscripciones - Zabora")
            .version("1.0.0")
            .contact(contact)
            .description("Microservicio para gestionar suscripciones con integración de Stripe")
            .license(mitLicense);
        
        // Esquema de seguridad JWT
        SecurityScheme securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");
        
        // Requerimiento de seguridad
        SecurityRequirement securityRequirement = new SecurityRequirement()
            .addList("bearerAuth");
        
        // Componentes
        Components components = new Components()
            .addSecuritySchemes("bearerAuth", securityScheme);
        
        return new OpenAPI()
            .info(info)
            .servers(List.of(server))
            .components(components)
            .addSecurityItem(securityRequirement);
    }
}