package com.jobassistant.util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class HelperMethods {

    public void parseEmail(String emailJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(emailJson);

            JsonNode headers = root.path("payload").path("headers");

            String subject = "";
            String from = "";
            String date = "";

            for (JsonNode header : headers) {
                String name = header.get("name").asText();

                if (name.equalsIgnoreCase("Subject")) {
                    subject = header.get("value").asText();
                } else if (name.equalsIgnoreCase("From")) {
                    from = header.get("value").asText();
                } else if (name.equalsIgnoreCase("Date")) {
                    date = header.get("value").asText();
                }
            }

            System.out.println("Subject: " + subject);
            System.out.println("From: " + from);
            System.out.println("Date: " + date);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
