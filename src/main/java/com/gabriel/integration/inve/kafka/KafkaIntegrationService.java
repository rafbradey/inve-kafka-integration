package com.gabriel.integration.inve.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KafkaIntegrationService {

    private final Set<String> sentItems = new HashSet<>();

    public String sendToAPI(String supportFilename) throws Exception {
        String message = null;
        if (sentItems.contains(supportFilename)) {
            return "Item already sent";
        }

        String XMLPath = "src/main/java/com/gabriel/integration/inve/kafka/dumpedXML/" + supportFilename + ".xml";
        String supportXMLPayload = new String(Files.readAllBytes(Paths.get(XMLPath)));

        String supportName = supportXMLPayload.substring(supportXMLPayload.indexOf("<name>") + 6, supportXMLPayload.indexOf("</name>"));
        System.out.println(supportFilename + " name: " + supportName);

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

        int highestId = getHighestId(supportFilename);

        message = "Added a value in " + supportFilename + " [Name: " + supportName + "] with ID " + highestId;
        sentItems.add(supportFilename); // Track the sent item

        connection.disconnect();
        return message;
    }

    private URL getSupportURL(String supportFilename) throws Exception {
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

        try (InputStream inputStream = connection.getInputStream()) {
            String response = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

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
}
