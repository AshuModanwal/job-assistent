package com.jobassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.entity.JobApplication;
import com.jobassistant.entity.Users;
import com.jobassistant.repository.JobApplicationRepository;
import com.jobassistant.util.HelperMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JobService {

    @Autowired
    private GmailService gmailService;

    @Autowired
    private HelperMethods helper;

    @Autowired
    private JobApplicationRepository jobRepo;

    public List<JobApplication> syncJobs(String token, Users user) {

        List<JobApplication> savedJobs = new ArrayList<>();

        String response = gmailService.fetchEmails(token);

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(response);
            JsonNode messages = root.path("messages");

            for (JsonNode msg : messages) {

                String messageId = msg.get("id").asText();

                // avoid duplicate
                if (jobRepo.existsByEmailId(messageId)) continue;

                String emailJson = gmailService.getEmailDetails(token, messageId);

                Map<String, String> parsed = helper.parseEmail(emailJson);

                String subject = parsed.get("subject");
                String snippet = parsed.get("snippet");

                // 🔥 JOB FILTER LOGIC
                if (isJobEmail(subject, snippet)) {

                    JobApplication job = new JobApplication();
                    job.setEmailId(messageId);
                    job.setCompany(extractCompany(subject));
                    job.setRole(extractRole(subject));
                    job.setSource(extractSource(parsed.get("from")));
                    job.setUser(user);

                    jobRepo.save(job);
                    savedJobs.add(job);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedJobs;
    }

    private boolean isJobEmail(String subject, String snippet) {
        String text = (subject + " " + snippet).toLowerCase();

        return text.contains("application") ||
                text.contains("interview") ||
                text.contains("thank you for applying");
    }

    private String extractCompany(String subject) {
        return subject; // improve later
    }

    private String extractRole(String subject) {
        return subject; // improve later
    }

    private String extractSource(String from) {
        if (from == null) return "Unknown";

        if (from.toLowerCase().contains("linkedin")) return "LinkedIn";
        if (from.toLowerCase().contains("indeed")) return "Indeed";

        return "Direct";
    }
}
