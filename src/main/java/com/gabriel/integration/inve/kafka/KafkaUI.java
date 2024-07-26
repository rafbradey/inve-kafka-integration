package com.gabriel.integration.inve.ui;

import com.gabriel.integration.inve.kafka.KafkaProducer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KafkaUI extends Application {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Override
    public void start(Stage primaryStage) {
        TextField messageField = new TextField();
        Button sendButton = new Button("Send");

        sendButton.setOnAction(e -> {
            String message = messageField.getText();
            kafkaProducer.sendNotification(message);
            //check if the message is sent
            if (message != null) {
                System.out.println("Message sent to Kafka: " + message);
            }

        });

        VBox vbox = new VBox(messageField, sendButton);
        Scene scene = new Scene(vbox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
