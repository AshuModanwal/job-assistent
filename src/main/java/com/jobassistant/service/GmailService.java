package com.jobassistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages";

    public String fetchEmails(String accessToken, String query, int maxResults) {

        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages"
                + "?q=" + query
                + "&maxResults=" + maxResults;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }

    // ✅ Fetch single email details
    public String getEmailDetails(String accessToken, String messageId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/" + messageId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch email details", e);
        }
    }
}