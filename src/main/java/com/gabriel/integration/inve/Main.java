package com.gabriel.integration.inve;

import com.gabriel.integration.inve.service.InventoryService;
import com.gabriel.integration.inve.service.ProductService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduling support
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the port address");
            System.exit(1); // Exit with a non-zero status code to indicate an error
        }
        String port = args[0];
        System.out.println("Starting application with port: " + port);

        try {
            InventoryService.getService(port);
            ProductService.getService(port);
        } catch (Exception e) {
            System.err.println("Error initializing InventoryService: " + e.getMessage());
            e.printStackTrace();
            System.exit(1); // Exit with a non-zero status code to indicate an error
        }

        SpringApplication.run(Main.class, args);
        System.out.println("Application started successfully");
    }
}
