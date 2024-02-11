package com.manh.custom.target.generator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.manh.custom.target.generator.service.StorageProperties;
import com.manh.custom.target.generator.service.StorageService;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class CustomTargetGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomTargetGeneratorApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}
}
