package com.jobassistant.util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Component
public class HelperMethods {

    public Map<String, String> parseEmail(String emailJson) {
        Map<String, String> result = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(emailJson);

            JsonNode headers = root.path("payload").path("headers");

            for (JsonNode header : headers) {
                String name = header.get("name").asText();

                if (name.equalsIgnoreCase("Subject")) {
                    result.put("subject", header.get("value").asText());
                } else if (name.equalsIgnoreCase("From")) {
                    result.put("from", header.get("value").asText());
                } else if (name.equalsIgnoreCase("Date")) {
                    result.put("date", header.get("value").asText());
                }
            }

            result.put("snippet", root.path("snippet").asText());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
