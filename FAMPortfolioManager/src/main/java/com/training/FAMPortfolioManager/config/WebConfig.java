package com.training.FAMPortfolioManager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// WebConfig - CORS and Web Configuration
// Annotate with @Configuration
// Implement WebMvcConfigurer
// Override addCorsMappings() to allow requests from http://localhost:3000 (or wherever frontend runs)
//
// IMPORTS NEEDED:
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
@Configuration
public class WebConfig {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}