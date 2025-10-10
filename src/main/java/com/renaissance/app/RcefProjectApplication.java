package com.renaissance.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RcefProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(RcefProjectApplication.class, args);
	}

}
