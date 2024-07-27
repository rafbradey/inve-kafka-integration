package com.gabriel.integration.inve;

import com.gabriel.integration.inve.kafka.KAFKA_NOTIFICATION_UI;
import com.gabriel.integration.inve.service.InventoryService;
import com.gabriel.integration.inve.service.CategoryService;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MainApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the port address");
            System.exit(1);
        }
        String port = args[0];
        System.out.println("Starting application with port: " + port);

        ConfigurableApplicationContext context = SpringApplication.run(MainApp.class, args);

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
                KAFKA_NOTIFICATION_UI.setContext(context); // Pass the Spring context to KAFKA_NOTIFICATION_UI
                Application.launch(KAFKA_NOTIFICATION_UI.class, args); // Launch JavaFX application
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Application started successfully");
    }
}
