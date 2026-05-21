package com.topik.topikai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class TopikaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TopikaiApplication.class, args);
	}

}
