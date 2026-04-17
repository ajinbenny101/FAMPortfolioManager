package com.training.FAMPortfolioManager.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// WebConfig - CORS and Web Configuration
// Annotate with @Configuration
// Implement WebMvcConfigurer
// Override addCorsMappings() to allow requests from http://localhost:3000 (or wherever frontend runs)
//
// IMPORTS NEEDED:
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;

// This configuration class sets up CORS to allow cross-origin requests from the frontend application
// and also defines a RestTemplate bean for making HTTP requests to external APIs.

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Configure CORS to allow requests from the frontend application
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    // RestTemplate bean for making HTTP requests to external APIs (e.g., price service)
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(requestFactory);
    }
}