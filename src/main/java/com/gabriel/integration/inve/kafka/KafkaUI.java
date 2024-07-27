package com.gabriel.integration.inve.kafka;

import com.gabriel.integration.inve.model.Product;
import com.gabriel.integration.inve.service.ProductService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class KafkaUI extends Application {

    private static ApplicationContext context;

    public static void main(String[] args) {
        // Initialize Spring context
        context = new AnnotationConfigApplicationContext(KafkaApplication.class);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        KafkaProducer kafkaProducer = context.getBean(KafkaProducer.class);

        Button sendButton = new Button("Send");
        sendButton.setPrefWidth(100);

        sendButton.setOnAction(e -> {
            String message = sendToAPI();
            kafkaProducer.sendNotification(message);

            // Check if the message is sent
            if (message != null && !message.isEmpty()) {
                System.out.println("Message sent to Kafka: " + message);
            }
        });

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(sendButton);

        Scene scene = new Scene(vbox, 400, 200); // Adjusted scene dimensions
        primaryStage.setTitle("Kafka UI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public String sendToAPI() {
        String message = null;
        try {
            // Read XML file
            String xmlPayload = new String(Files.readAllBytes(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/Product.xml")));


            String productName = xmlPayload.substring(xmlPayload.indexOf("<name>") + 6, xmlPayload.indexOf("</name>"));
            System.out.println("Product name: " + productName);

            ProductService product = new ProductService();
            // Prepare PUT request in http://localhost:8080/api/product
            URL productURL = new URL(product.getProductURL().toString());

            HttpURLConnection connection = (HttpURLConnection) productURL.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(xmlPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            connection.disconnect();

            // Prepare notification message
            message = "[Name: " + productName +"]";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @Override
    public void stop() throws Exception {
        // Cleanup Spring context if needed
        ((AnnotationConfigApplicationContext) context).close();
        super.stop();
    }
}
