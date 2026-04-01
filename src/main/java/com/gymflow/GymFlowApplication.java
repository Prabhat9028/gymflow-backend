package com.gymflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(GymFlowApplication.class, args);
    }
}
