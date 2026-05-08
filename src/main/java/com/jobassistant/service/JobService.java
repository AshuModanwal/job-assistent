package com.jobassistant.service;

import com.jobassistant.dto.response.application.JobApplicationResponse;
import com.jobassistant.dto.response.application.SyncResponse;
import com.jobassistant.enums.ApplicationStatus;
import com.jobassistant.entity.JobApplication;
import com.jobassistant.entity.Users;
import com.jobassistant.repository.JobApplicationRepository;
import com.jobassistant.util.EmailClassifier;
import com.jobassistant.util.EmailParser;
import com.jobassistant.util.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final GmailService gmailService;
    private final EmailParser emailParser;
    private final EmailClassifier emailClassifier;
    private final ScoreCalculator scoreCalculator;
    private final JobApplicationRepository jobRepo;

    private static final String GMAIL_QUERY =
            "category:primary (subject:(application OR interview OR offer OR shortlisted OR selected OR rejected OR thank you for applying OR assessment OR coding challenge))";

    @Transactional
    public SyncResponse sync(String accessToken, Users user) {
        List<String> messageIds = gmailService.listMessageIds(accessToken, GMAIL_QUERY, 50);

        int totalFetched      = messageIds.size();
        int newlySaved        = 0;
        int skippedDuplicates = 0;
        int skippedNoise      = 0;
        List<JobApplicationResponse> saved = new ArrayList<>();

        log.info("Sync started for user={} | fetched {} message IDs", user.getEmail(), totalFetched);

        for (String messageId : messageIds) {
            if (jobRepo.existsByEmailId(messageId)) {
                skippedDuplicates++;
                continue;
            }

            try {
                String raw = gmailService.getMessageDetail(accessToken, messageId);
                Map<String, String> parsed = emailParser.parse(raw);

                String subject    = parsed.get("subject");
                String snippet   = parsed.get("snippet");
                String from      = parsed.get("from");
                String dateHeader = parsed.get("date");

                // Skip platform spam senders
                if (emailClassifier.isSenderNoise(from)) {
                    log.debug("Skipped noise sender: {}", from);
                    skippedNoise++;
                    continue;
                }

                ApplicationStatus status = emailClassifier.classify(subject, snippet);
                if (status == null) {
                    log.debug("Skipped unrecognised email: {}", subject);
                    skippedNoise++;
                    continue;
                }

                double score = scoreCalculator.calculate(status, subject, snippet);

                JobApplication job = JobApplication.builder()
                        .emailId(messageId)
                        .company(emailParser.extractCompany(subject, from))
                        .role(emailParser.extractRole(subject))
                        .source(emailParser.extractSource(from))
                        .status(status)
                        .score(score)
                        .appliedDate(emailParser.extractDate(dateHeader))
                        .rawSubject(subject)
                        .rawSnippet(snippet)
                        .user(user)
                        .build();

                jobRepo.save(job);
                saved.add(toResponse(job));
                newlySaved++;

                log.info("Saved: {} | {} | {}", status, job.getCompany(), job.getRole());

            } catch (Exception e) {
                log.error("Error processing message {}: {}", messageId, e.getMessage());
            }
        }

        log.info("Sync done | new={} | dup={} | noise={}", newlySaved, skippedDuplicates, skippedNoise);

        return SyncResponse.builder()
                .totalFetched(totalFetched)
                .newlySaved(newlySaved)
                .skippedDuplicates(skippedDuplicates)
                .skippedNoise(skippedNoise)
                .saved(saved)
                .build();
    }

    public List<JobApplicationResponse> getAllForUser(Users user) {
        return jobRepo.findByUserOrderByAppliedDateDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<JobApplicationResponse> getRecentForUser(Users user) {
        return jobRepo.findTop10ByUserOrderByAppliedDateDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public JobApplicationResponse toResponse(JobApplication j) {
        return JobApplicationResponse.builder()
                .id(j.getId())
                .company(j.getCompany())
                .role(j.getRole())
                .source(j.getSource())
                .appliedDate(j.getAppliedDate())
                .status(j.getStatus())
                .score(j.getScore())
                .feedback(j.getFeedback())
                .build();
    }
}