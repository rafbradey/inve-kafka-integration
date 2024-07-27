package com.gabriel.integration.inve.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabriel.integration.inve.service.CategoryService;
import com.gabriel.integration.inve.service.InventoryService;
import com.gabriel.integration.inve.service.StatusService;
import com.gabriel.integration.inve.service.StorageService;
import com.gabriel.integration.inve.service.SupplierService;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Collectors;

public class KafkaUI extends Application {

    private static ApplicationContext context;

    public static void setContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    @Override
    public void start(Stage primaryStage) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext not set.");
        }

        KafkaProducer kafkaProducer = context.getBean(KafkaProducer.class);

        Button sendButton = new Button("Send");
        sendButton.setPrefWidth(100);
        ListView<String> xmlList = new ListView<>();
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
            try {
                String supportFilename = getSupportFilenameFromSelection(xmlList.getSelectionModel().getSelectedItem());
                String message = sendToAPI(supportFilename);
                kafkaProducer.sendNotification(message);

                // Alert for success
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText("Message Sent");
                successAlert.setContentText("Message sent to Kafka: " + message);
                successAlert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();

                // Alert for error
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Message Sending Failed");
                errorAlert.setContentText("Failed to send message to Kafka: " + ex.getMessage());
                errorAlert.showAndWait();
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

    private String getSupportFilenameFromSelection(String selectedItem) {

        return selectedItem != null ? selectedItem.replace(".xml", "") : "Category";
    }

    public String sendToAPI(String supportFilename) {
        String message = null;
        try {
            String XMLPath = "src/main/java/com/gabriel/integration/inve/kafka/dumpedXML/" + supportFilename + ".xml";
            String supportXMLPayload = new String(Files.readAllBytes(Paths.get(XMLPath)));

            String supportName = supportXMLPayload.substring(supportXMLPayload.indexOf("<name>") + 6, supportXMLPayload.indexOf("</name>"));
            System.out.println(supportFilename + " name: " + supportName);

            // Get the URL and service dynamically
            URL supportURL = getSupportURL(supportFilename);
            HttpURLConnection connection = (HttpURLConnection) supportURL.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(supportXMLPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Retrieve the highest ID
            int highestId = getHighestId(supportFilename);

            // Prepare notification message
            message = "!Notification from User! - New item added: " + supportFilename + " [Name: " + supportName + "] with ID " + highestId
                    + "at " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(new Date());

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    private URL getSupportURL(String supportFilename) throws Exception {
        // Return the URL based on the support filename
        String baseUrl = "http://localhost:8080/api/";
        switch (supportFilename.toLowerCase()) {
            case "category":
                return new URL(baseUrl + "category");
            case "status":
                return new URL(baseUrl + "status");
            case "storage":
                return new URL(baseUrl + "storage");
            case "supplier":
                return new URL(baseUrl + "supplier");
            default:
                throw new IllegalArgumentException("Unsupported filename: " + supportFilename);
        }
    }

    private int getHighestId(String supportFilename) throws Exception {
        String url = "http://localhost:8080/api/" + supportFilename.toLowerCase();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        // Read the response
        try (InputStream inputStream = connection.getInputStream()) {
            String response = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));

            // Print the response for debugging
          //  System.out.println("Response from GET request:");
         //   System.out.println(response);

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            // Print JSON tree for debugging
          //  System.out.println("Parsed JSON:");
          //  System.out.println(root.toPrettyString());

            // Assuming the response is a JSON array
            int highestId = 0;
            if (root.isArray()) {
                for (JsonNode item : root) {
                    if (item.has("id") && item.get("id").isInt()) {
                        int id = item.get("id").asInt();
                        if (id > highestId) {
                            highestId = id;
                        }
                    }
                }
            }

            connection.disconnect();
            return highestId;
        }
    }




    @Override
    public void stop() throws Exception {
        // Cleanup Spring context if needed
        if (context != null && context instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext) context).close();
        }
        super.stop();
    }
}
