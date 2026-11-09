package com.example.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI 3 / Swagger pour documenter l'API REST TaskBoard.
 * Configure notamment le schéma de sécurité JWT Bearer pour permettre
 * de tester les routes protégées directement depuis l'interface Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskBoard API")
                        .version("1.0.0")
                        .description("API REST de gestion de projets et tâches (TaskBoard) sécurisée par JSON Web Token (JWT).")
                        .contact(new Contact()
                                .name("Formation Fullstack")
                                .email("contact@ecoledecode.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // Applique l'exigence de sécurité globalement dans Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                // Déclare le mécanisme d'authentification Bearer JWT
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insérez votre token JWT (obtenu via l'endpoint /login) pour autoriser vos requêtes.")));
    }
}