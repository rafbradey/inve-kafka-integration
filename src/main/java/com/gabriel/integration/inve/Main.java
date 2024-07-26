package com.gabriel.integration.inve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduling support
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
