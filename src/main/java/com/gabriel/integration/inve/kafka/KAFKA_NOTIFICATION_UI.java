package com.gabriel.integration.inve.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabriel.integration.inve.service.CategoryService;
import com.gabriel.integration.inve.service.InventoryService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
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
import java.util.Date;
import java.util.stream.Collectors;

public class KAFKA_NOTIFICATION_UI extends Application {

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
        sendButton.setPrefWidth(200);
        sendButton.setPrefHeight(50);
        ListView<String> xmlList = new ListView<>();
        xmlList.setPrefSize(300, 150);

        TextArea textArea = new TextArea();
        textArea.setPrefSize(500, 250);
        textArea.setEditable(false);

        try {
            Files.list(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/dumped_FROM_ERP_XML"))
                    .filter(Files::isRegularFile)
                    .forEach(file -> xmlList.getItems().add(file.getFileName().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        xmlList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            try {
                textArea.setText(new String(Files.readAllBytes(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/dumped_FROM_ERP_XML/" + newValue))));
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
                //change alert width
                successAlert.getDialogPane().setPrefWidth(600);
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



        //Show List view
        Label title = new Label("List of Dumped XML Files from the ERP");
        //set the title style
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        
        Label sentStatus = new Label();

        if (xmlList.getItems().isEmpty()) {
            sentStatus.setText("No XML files found in the directory");
        } else {
            sentStatus.setText("Select an XML file to send to the API");
        }

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(sendButton);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        GridPane.setConstraints(title, 0, 0, 2, 1);
        gridPane.getChildren().add(title);

        GridPane.setConstraints(xmlList, 0, 1);
        gridPane.getChildren().add(xmlList);

        GridPane.setConstraints(sentStatus, 0,2,2,1);
        gridPane.getChildren().add(sentStatus);

        GridPane.setConstraints(textArea, 1, 1);
        gridPane.getChildren().add(textArea);


        GridPane.setConstraints(buttonBox, 0, 2, 2, 1);
        gridPane.getChildren().add(buttonBox);


        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(20));
        mainVBox.setAlignment(Pos.CENTER);
        mainVBox.getChildren().add(gridPane);

        Scene scene = new Scene(mainVBox, 800, 400);
        primaryStage.setTitle("Kafka Notification UI");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText("The Scheduler is still running in the background");
            alert.setContentText("You may stop the scheduler by pressing Ctrl + C in the terminal");
            alert.showAndWait();
            primaryStage.close();
        });



    }

    private String getSupportFilenameFromSelection(String selectedItem) {

        return selectedItem != null ? selectedItem.replace(".xml", "") : "Inventory";
    }

    public String sendToAPI(String supportFilename) {
        String message = null;
        try {
            String XMLPath = "src/main/java/com/gabriel/integration/inve/kafka/dumped_FROM_ERP_XML/" + supportFilename + ".xml";
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
            message = "!!!Notification received from a User! - New item added: " + supportFilename + " [Name: " + supportName + "] with ID " + highestId
                    + " Received at: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(new Date());

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    private URL getSupportURL(String supportFilename) throws Exception {
        // Return the URL based on the support filename
        InventoryService inventory = new InventoryService();
        String baseUrl = "http://localhost:"+inventory.getInventoryPort() +"/api/";
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
        InventoryService inventory = new InventoryService();
        String url = "http://localhost:"+inventory.getInventoryPort() +"/api/" + supportFilename.toLowerCase();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        // Read the response
        try (InputStream inputStream = connection.getInputStream()) {
            String response = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));


            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);


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
