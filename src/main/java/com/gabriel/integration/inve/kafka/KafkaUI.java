package com.gabriel.integration.inve.kafka;

import com.gabriel.integration.inve.model.Product;
import com.gabriel.integration.inve.service.ProductService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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
        ListView<String> xmlList = new ListView();
        xmlList.setPrefSize(300, 150);

        TextArea textArea = new TextArea();
        textArea.setPrefSize(300, 250);
        textArea.setEditable(false);

        try {
            Files.list(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/dumpedXML"))
                    .filter(Files::isRegularFile)
                    .forEach(file -> xmlList.getItems().add(file.getFileName().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        xmlList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            try {
                textArea.setText(new String(Files.readAllBytes(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/dumpedXML/" + newValue))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        sendButton.setOnAction(e -> {
            String message = sendToAPI();
            kafkaProducer.sendNotification(message);

            // Check if the message is sent
            if (message != null && !message.isEmpty()) {
                System.out.println("Message sent to Kafka: " + message);
            }
        });

// Show list view
        VBox vboxLT = new VBox(10);
        vboxLT.setPadding(new Insets(20));
        vboxLT.setAlignment(Pos.CENTER);


        Label title = new Label("List of Dumped XML Files from the ERP");
        vboxLT.getChildren().add(title);
        vboxLT.getChildren().add(xmlList);
        vboxLT.getChildren().add(textArea);




        HBox hbox = new HBox();
        hbox.setPadding(new Insets(20));
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().add(vboxLT);

        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(20));
        mainVBox.setAlignment(Pos.CENTER);
        mainVBox.getChildren().addAll(hbox, sendButton);

        Scene scene = new Scene(mainVBox, 400, 600);
        primaryStage.setTitle("Kafka UI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public String sendToAPI() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successefully Published");
        alert.setHeaderText("Notification sent to Kafka");
        alert.showAndWait();


        String message = null;
        try {
            // Read XML file
            String productxmlPayload = new String(Files.readAllBytes(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/dumpedXML/Product.xml")));


            String productName = productxmlPayload.substring(productxmlPayload.indexOf("<name>") + 6, productxmlPayload.indexOf("</name>"));
            System.out.println("Product name: " + productName);

            ProductService product = new ProductService();
            // Prepare PUT request in http://localhost:8080/api/product
            URL productURL = new URL(product.getProductURL().toString());

            HttpURLConnection connection = (HttpURLConnection) productURL.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(productxmlPayload.getBytes());
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
