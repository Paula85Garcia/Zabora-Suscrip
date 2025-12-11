package com.zabora.subscription.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI / Swagger para la documentación de la API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zaboraOpenAPI() {
        // Configuración del servidor
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Servidor de Desarrollo");

        // Información de contacto
        Contact contact = new Contact()
                .name("Equipo Zabora")
                .email("soporte@zabora.com")
                .url("https://www.zabora.com");

        // Licencia del proyecto
        License mitLicense = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        // Información general de la API
        Info info = new Info()
                .title("API de Suscripciones - Zabora")
                .version("1.0.0")
                .description("Microservicio para gestionar suscripciones de la plataforma Zabora")
                .contact(contact)
                .license(mitLicense);

        // Retornar configuración completa de OpenAPI
        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}

