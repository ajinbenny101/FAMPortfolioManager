package com.training.FAMPortfolioManager.config;

// SwaggerConfig - API Documentation Configuration
// Annotate with @Configuration
// Create a @Bean that returns an OpenAPI object with a title and description
// This auto-generates your API docs at /swagger-ui.html
//
// IMPORTS NEEDED:
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Bean;
// import io.swagger.v3.oas.models.OpenAPI;
// import io.swagger.v3.oas.models.info.Info;
// IMPORTS NEEDED:
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Manager API")
                        .version("1.0")
                        .description("REST API for managing financial portfolios with live price data"));
    }
}