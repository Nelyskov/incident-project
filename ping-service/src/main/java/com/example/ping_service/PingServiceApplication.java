package com.example.ping_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PingServiceApplication.class, args);
	}

}
