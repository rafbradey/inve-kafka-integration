package com.gabriel.integration.inve;

import com.gabriel.integration.inve.kafka.KafkaApplication;
import com.gabriel.integration.inve.kafka.KafkaProducer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class KafkaUI extends Application {

    private static ApplicationContext context;

    @Autowired
    private KafkaProducer kafkaProducer;

    public static void main(String[] args) {
        // Initialize Spring context
        context = new AnnotationConfigApplicationContext(KafkaApplication.class);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize KafkaProducer from Spring context
        kafkaProducer = context.getBean(KafkaProducer.class);

        TextField messageField = new TextField();
        messageField.setPromptText("Enter message here");
        messageField.setPrefSize(100, 20);

        Button sendButton = new Button("Send");
        sendButton.setPrefSize(100, 20);
        sendButton.setLayoutX(40);


        sendButton.setOnAction(e -> {
            String message = messageField.getText();
            kafkaProducer.sendNotification(message);
            // Check if the message is sent
            if (message != null) {
                System.out.println("Message sent to Kafka: " + message);
            }
        });

        VBox vbox = new VBox(messageField);
        Pane pane = new Pane();
       vbox.getChildren().addAll(sendButton);
        Scene scene = new Scene(vbox, 500, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Cleanup Spring context if needed
        ((AnnotationConfigApplicationContext) context).close();
        super.stop();
    }
}
