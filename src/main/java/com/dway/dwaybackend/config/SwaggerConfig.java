package com.dway.dwaybackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dway API")
                        .version("v1.0.0")
                        .description("Dway mobile and admin REST API")
                        .contact(new Contact()
                                .name("Dway Team")
                                .email("support@dway.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token")));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {

            RequestMapping classMapping = handlerMethod
                    .getMethod()
                    .getDeclaringClass()
                    .getAnnotation(RequestMapping.class);

            String basePath = (classMapping != null && classMapping.value().length > 0)
                    ? classMapping.value()[0]
                    : "";

            boolean isPublic = basePath.startsWith("/api/v1/auth");

            if (!isPublic) {
                operation.addSecurityItem(
                        new SecurityRequirement().addList("bearerAuth"));
            }

            return operation;
        };
    }
}