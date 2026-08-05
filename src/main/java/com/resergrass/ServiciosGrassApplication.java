package com.resergrass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiciosGrassApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiciosGrassApplication.class, args);
    }
}
