package com.example.incedent_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.incedent_service.repositories")
public class IncedentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncedentServiceApplication.class, args);
	}
}