package com.setu.cloudbilling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudbillingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudbillingApplication.class, args);
	}

}
