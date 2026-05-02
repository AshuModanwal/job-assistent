package com.jobassistant.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    // ✅ NEW: STATUS DETECTION
    public String extractStatus(String subject, String snippet) {
        String text = (subject + " " + snippet).toLowerCase();

        if (text.contains("interview")) return "INTERVIEW";
        if (text.contains("selected")) return "INTERVIEW";
        if (text.contains("shortlisted")) return "INTERVIEW";
        if (text.contains("thank you for applying")) return "APPLIED";

        return "NO_RESPONSE";
    }

    // ✅ NEW: SOURCE DETECTION
    public String extractSource(String from) {

        if (from == null) return "Direct";

        String lower = from.toLowerCase();

        if (lower.contains("linkedin")) return "LinkedIn";
        if (lower.contains("indeed")) return "Indeed";
        if (lower.contains("naukri")) return "Naukri";

        return "Direct";
    }

    // ✅ NEW: BASIC COMPANY EXTRACTION
    public String extractCompany(String subject) {
        if (subject == null) return "Unknown";

        return subject.split(" ")[0]; // simple for now
    }

    // ✅ NEW: ROLE EXTRACTION (basic)
    public String extractRole(String subject) {
        if (subject == null) return "Unknown";

        return subject;
    }

    // ✅ NEW: DATE PARSE (simplified)
    public LocalDate extractDate() {
        return LocalDate.now(); // later improve parsing
    }
}