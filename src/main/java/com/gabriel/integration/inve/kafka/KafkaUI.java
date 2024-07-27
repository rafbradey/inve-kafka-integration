package com.gabriel.integration.inve.kafka;

import com.gabriel.integration.inve.service.KafkaIntegrationService;
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

import java.nio.file.Files;
import java.nio.file.Paths;

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

        KafkaIntegrationService kafkaIntegrationService = context.getBean(KafkaIntegrationService.class);

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
                String message = kafkaIntegrationService.sendToAPI(supportFilename);

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

    @Override
    public void stop() throws Exception {
        if (context != null && context instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext) context).close();
        }
        super.stop();
    }
}
