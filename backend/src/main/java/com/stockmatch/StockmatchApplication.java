package com.stockmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class StockmatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockmatchApplication.class, args);
	}

}
