package com.gabriel.integration.inve;

import com.gabriel.integration.inve.kafka.KafkaUI;
import com.gabriel.integration.inve.service.InventoryService;
import com.gabriel.integration.inve.service.CategoryService;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the port address");
            System.exit(1);
        }
        String port = args[0];
        System.out.println("Starting application with port: " + port);

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

        try {
            InventoryService.getService(port);
            CategoryService.getService(port);
        } catch (Exception e) {
            System.err.println("Error initializing InventoryService: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Start the JavaFX application on a new thread
        new Thread(() -> {
            try {
                KafkaUI.setContext(context); // Pass the Spring context to KafkaUI
                Application.launch(KafkaUI.class, args); // Launch JavaFX application
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Application started successfully");
    }
}
