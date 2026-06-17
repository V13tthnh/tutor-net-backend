package com.tutornet.tutor_net;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TutorNetApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutorNetApplication.class, args);
	}

}
