package com.gabriel.integration.inve.kafka;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send("InventoryTopic", message);
    }

    public void sendNotification(String message) {
        kafkaTemplate.send("InventoryTopic", message);


    }
}
