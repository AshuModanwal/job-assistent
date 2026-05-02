package com.jobassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.entity.JobApplication;
import com.jobassistant.entity.Users;
import com.jobassistant.repository.JobApplicationRepository;
import com.jobassistant.util.HelperMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class JobService {

    private final GmailService gmailService;
    private final HelperMethods helper;
    private final JobApplicationRepository jobRepo;

    // ✅ SYNC JOBS FROM GMAIL
    public List<JobApplication> syncJobs(String token, Users user) {

        List<JobApplication> savedJobs = new ArrayList<>();

        String response = gmailService.fetchEmails(token);
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(response);
            JsonNode messages = root.path("messages");

            for (JsonNode msg : messages) {

                String messageId = msg.get("id").asText();

                if (jobRepo.existsByEmailId(messageId)) continue;

                String emailJson = gmailService.getEmailDetails(token, messageId);
                Map<String, String> parsed = helper.parseEmail(emailJson);

                String subject = parsed.get("subject");
                String snippet = parsed.get("snippet");

                if (isJobEmail(subject, snippet)) {

                    JobApplication job = JobApplication.builder()
                            .emailId(messageId)
                            .company(helper.extractCompany(subject))
                            .role(helper.extractRole(subject))
                            .source(helper.extractSource(parsed.get("from")))
                            .status(helper.extractStatus(subject, snippet))
                            .appliedDate(helper.extractDate())
                            .score(generateScore())
                            .user(user)
                            .build();

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

    // ✅ TEMP AI SCORE
    private double generateScore() {
        return 60 + new Random().nextInt(40);
    }

    // =========================
    // ✅ DASHBOARD APIs LOGIC
    // =========================

    public Map<String, Object> getStats(Users user) {

        Map<String, Object> stats = new HashMap<>();

        stats.put("total", jobRepo.countByUser(user));
        stats.put("interviews", jobRepo.countByUserAndStatus(user, "INTERVIEW"));
        stats.put("noResponse", jobRepo.countByUserAndStatus(user, "NO_RESPONSE"));
        stats.put("responses", jobRepo.countByUserAndStatusNot(user, "NO_RESPONSE"));

        return stats;
    }

    public List<Map<String, Object>> getSourceStats(Users user) {

        List<Object[]> data = jobRepo.getSourceStats(user);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : data) {
            Map<String, Object> map = new HashMap<>();
            map.put("source", row[0]);
            map.put("count", row[1]);
            result.add(map);
        }

        return result;
    }

    public List<Map<String, Object>> getTimelineStats(Users user) {

        List<Object[]> data = jobRepo.getApplicationsPerMonth(user);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : data) {
            Map<String, Object> map = new HashMap<>();
            map.put("month", row[0]);
            map.put("count", row[1]);
            result.add(map);
        }

        return result;
    }

    public List<JobApplication> getRecentApplications(Users user) {
        return jobRepo.findTop10ByUserOrderByAppliedDateDesc(user);
    }
}