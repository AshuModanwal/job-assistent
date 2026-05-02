package com.jobassistant.controller;

import com.jobassistant.entity.JobApplication;
import com.jobassistant.entity.Users;
import com.jobassistant.repository.UserRepository;
import com.jobassistant.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;

    private Users getUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }

    // =========================
    // ✅ SYNC
    // =========================

    @GetMapping("/sync")
    public List<JobApplication> sync(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        String token = client.getAccessToken().getTokenValue();
        Users user = getUser(oAuth2User);

        return jobService.syncJobs(token, user);
    }

    // =========================
    // ✅ DASHBOARD APIs
    // =========================

    @GetMapping("/stats")
    public Map<String, Object> stats(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return jobService.getStats(getUser(oAuth2User));
    }

    @GetMapping("/analytics/source")
    public List<Map<String, Object>> sourceStats(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return jobService.getSourceStats(getUser(oAuth2User));
    }

    @GetMapping("/analytics/timeline")
    public List<Map<String, Object>> timeline(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return jobService.getTimelineStats(getUser(oAuth2User));
    }

    @GetMapping("/recent")
    public List<JobApplication> recent(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return jobService.getRecentApplications(getUser(oAuth2User));
    }

    // =========================
    // ✅ ALL JOBS
    // =========================

    @GetMapping
    public List<JobApplication> getAll(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return getUser(oAuth2User).getJobApplications();
    }
}