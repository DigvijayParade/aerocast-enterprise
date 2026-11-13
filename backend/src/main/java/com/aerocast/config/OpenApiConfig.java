package com.aerocast.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AeroCast UK - Enterprise Energy Trading API")
                        .version("2.0.0")
                        .description("High-performance Spring Boot 3 microservice ingesting UK National Grid Elexon BMRS renewable wind generation and forecasts for day-ahead procurement balancing.")
                        .contact(new Contact().name("AeroCast Energy Trading Desk").email("support@aerocast.energy"))
                        .license(new License().name("Apache 2.0")));
    }
}
