package com.gabriel.integration.inve.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabriel.integration.inve.kafka.KafkaProducer;
import com.gabriel.integration.inve.service.CategoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Scheduler {

    private Set<String> existingNames = new HashSet<>();
    private final KafkaProducer kafkaProducer;

    public Scheduler(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(cron = "*/15 * * * * *") // Send Notif per 15 seconds
    public void scheduleTask() throws MalformedURLException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
        String strDate = dateFormat.format(new Date());

        System.out.println("Cron job Scheduler: Job running at - " + strDate);

        CategoryService categoryService = new CategoryService();
        String port = categoryService.getCategoryURL().toString();
        //--for debuggingSystem.out.println("Currently using: " + port);

        processXMLFiles();
    }

    private void processXMLFiles() {
        try {
            Files.list(Paths.get("src/main/java/com/gabriel/integration/inve/kafka/dumped_FROM_ERP_XML"))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String xmlContent = new String(Files.readAllBytes(file));
                            String supportFilename = file.getFileName().toString().replace(".xml", "");
                            String supportName = extractNameFromXML(xmlContent);

                            if (!existingNames.contains(supportName)) {
                                // Perform the PUT request
                                sendToAPI(supportFilename, xmlContent);

                                // Add the name to the set of existing names
                                existingNames.add(supportName);

                                // Send a notification to Kafka
                                String message = "!!!Notification received from Scheduler - New item added: " + supportFilename + " [Name: " + supportName + "] "
                                        + " Received at: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(new Date());
                                kafkaProducer.sendNotification(message);

                                // Print a message to System.out
                                System.out.println("New item processed and message sent to Kafka: " + message);
                            } else {
                                //System.out.println("Name already exists in the system: " + supportName);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractNameFromXML(String xmlContent) {
        // Extract name from XML content
        return xmlContent.substring(xmlContent.indexOf("<name>") + 6, xmlContent.indexOf("</name>"));
    }

    private void sendToAPI(String supportFilename, String xmlContent) throws Exception {
        String apiUrl = getAPIURL(supportFilename);
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/xml");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(xmlContent.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        System.out.println("PUT Request Response Code: " + responseCode);

        // Check if response is successful
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Handle success response
            System.out.println("Successfully sent XML to API: " + supportFilename);
        } else {
            // Handle error response
            System.out.println("Failed to send XML to API: " + supportFilename);
        }

        connection.disconnect();
    }

    private String getAPIURL(String supportFilename) throws Exception {
        String baseUrl = "http://localhost:8080/api/";
        switch (supportFilename.toLowerCase()) {
            case "category":
                return baseUrl + "category";
            case "status":
                return baseUrl + "status";
            case "storage":
                return baseUrl + "storage";
            case "supplier":
                return baseUrl + "supplier";
            default:
                throw new IllegalArgumentException("Unsupported filename: " + supportFilename);
        }
    }
}
