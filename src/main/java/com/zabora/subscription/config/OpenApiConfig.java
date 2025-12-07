package com.zabora.subscription.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI zaboraOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Servidor de Desarrollo");
        
        Contact contact = new Contact();
        contact.setEmail("soporte@zabora.com");
        contact.setName("Equipo Zabora");
        contact.setUrl("https://www.zabora.com");
        
        License mitLicense = new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT");
        
        Info info = new Info()
            .title("API de Suscripciones - Zabora")
            .version("1.0.0")
            .contact(contact)
            .description("Microservicio para gestionar suscripciones de la plataforma Zabora")
            .license(mitLicense);
        
        return new OpenAPI()
            .info(info)
            .servers(List.of(server));
    }
}
