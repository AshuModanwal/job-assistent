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

    public List<JobApplication> syncJobs(String token, Users user) {

        List<JobApplication> savedJobs = new ArrayList<>();

        // ✅ Gmail filtering + limit
        String response = gmailService.fetchEmails(
                token,
                "category:primary application OR interview OR job OR hiring OR offer letter",
                20
        );

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(response);
            JsonNode messages = root.path("messages");

            if (messages.isMissingNode() || messages.isEmpty()) {
                System.out.println("No messages found.");
                return savedJobs;
            }

            System.out.println("Total messages fetched: " + messages.size());

            for (JsonNode msg : messages) {

                String messageId = msg.get("id").asText();

                System.out.println("Processing message: " + messageId);

                if (jobRepo.existsByEmailId(messageId)) {
                    System.out.println("Already exists → skipped");
                    continue;
                }

                String emailJson = gmailService.getEmailDetails(token, messageId);
                Map<String, String> parsed = helper.parseEmail(emailJson);

                String subject = parsed.get("subject");
                String snippet = parsed.get("snippet");

                System.out.println("Subject: " + subject);
                System.out.println("Snippet: " + snippet);

                String from = parsed.get("from");

                if (from != null && (
                        from.toLowerCase().contains("naukri") ||
                                from.toLowerCase().contains("linkedin") ||
                                from.toLowerCase().contains("indeed")
                )) {
                    System.out.println("Ignored job platform spam");
                    continue;
                }

                String category = classifyEmail(subject, snippet);

                if (category.equals("IGNORE")) {
                    System.out.println("Ignored (not real application)");
                    continue;
                }

                JobApplication job = JobApplication.builder()
                        .emailId(messageId)
                        .company(helper.extractCompany(subject))
                        .role(helper.extractRole(subject))
                        .source(helper.extractSource(parsed.get("from")))
                        .status(category) // ✅ IMPORTANT CHANGE
                        .appliedDate(helper.extractDate())
                        .score(generateScore())
                        .user(user)
                        .build();

                jobRepo.save(job);
                savedJobs.add(job);

                System.out.println("Saved REAL job: " + category);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Total jobs saved: " + savedJobs.size());

        return savedJobs;
    }

    private boolean isJobEmail(String subject, String snippet) {

        if (subject == null && snippet == null) return false;

        String text = ((subject != null ? subject : "") + " " +
                (snippet != null ? snippet : "")).toLowerCase();

        return text.contains("application") ||
                text.contains("applied") ||
                text.contains("interview") ||
                text.contains("job") ||
                text.contains("hiring") ||
                text.contains("opportunity") ||
                text.contains("career") ||
                text.contains("position");
    }

    private String classifyEmail(String subject, String snippet) {

        if (subject == null && snippet == null) return "IGNORE";

        String text = ((subject != null ? subject : "") + " " +
                (snippet != null ? snippet : "")).toLowerCase();

        // ✅ OFFER (HIGH PRIORITY)
        if (text.contains("offer")) {
            return "OFFER";
        }

        // ✅ INTERVIEW
        if (text.contains("interview") ||
                text.contains("shortlisted") ||
                text.contains("assessment")) {
            return "INTERVIEW";
        }

        // ✅ APPLICATION CONFIRMATION (LESS STRICT)
        if (text.contains("applied") ||
                text.contains("application") ||
                text.contains("thank you")) {
            return "APPLIED";
        }

        // ❌ CLEAR NOISE
        if (text.contains("apply now") ||
                text.contains("jobs for you") ||
                text.contains("recommended") ||
                text.contains("weekly recap")) {
            return "IGNORE";
        }

        return "IGNORE";
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