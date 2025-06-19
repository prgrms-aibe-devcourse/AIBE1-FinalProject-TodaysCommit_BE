package com.team5.catdogeats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CatdogeatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatdogeatsApplication.class, args);
	}

}
