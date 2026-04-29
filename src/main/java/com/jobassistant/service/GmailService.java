package com.jobassistant.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class GmailService {

    public String fetchEmails(String accessToken) {
        try {
            URL url = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching emails";
        }
    }

    public String getEmailDetails(String accessToken, String messageId) {
        try {
            URL url = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching email details";
        }
    }
}
