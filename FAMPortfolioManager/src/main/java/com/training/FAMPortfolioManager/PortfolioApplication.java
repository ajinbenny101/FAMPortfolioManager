package com.training.FAMPortfolioManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// PortfolioApplication - main application class for the FAM Portfolio Manager
@SpringBootApplication
@EnableCaching
public class PortfolioApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfolioApplication.class, args);
	}

}