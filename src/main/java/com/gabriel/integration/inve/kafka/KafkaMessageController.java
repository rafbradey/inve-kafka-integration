package com.gabriel.integration.inve.kafka;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class KafkaMessageController
{
    private static ApplicationContext context;

    @Autowired
    KafkaProducer kafkaProducerService;
    public String publishMessage()
    {
        String message = "";
        kafkaProducerService.sendNotification(message);
        return "Success";
    }
}
