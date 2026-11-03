package com.lundi_m.spazacred_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("SpazaCred API")
                        .description("""
        SpazaCred is a RESTful API designed to support the day-to-day operations
        of small retail businesses and spaza shops.

        The platform provides a structured way to manage customers, credit accounts,
        purchases, payments and outstanding balances while maintaining financial
        accountability and operational transparency.

        Key Features:
        - Customer registration and management
        - Credit account creation and lifecycle management
        - Credit limit management and account status controls
        - Purchase recording and payment processing
        - Automatic payment allocation to outstanding transactions
        - Overdue account monitoring and suspension
        - Audit logging for traceability and accountability
        - Role-based access control for administrators, shop owners and employees
        - JWT-based authentication and password management
        - Financial reports and transaction summaries
        """)
                        .version("1.0.0")
                        .contact(new Contact()
                        .name("Keabetswe Mathabatha")
                        .email("keabetswelundim@gmail.com"))
                )
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}