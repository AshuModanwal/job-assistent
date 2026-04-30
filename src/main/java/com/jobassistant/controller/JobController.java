package com.jobassistant.controller;

import com.jobassistant.entity.JobApplication;
import com.jobassistant.entity.Users;
import com.jobassistant.repository.UserRepository;
import com.jobassistant.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/sync")
    public List<JobApplication> sync(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {

        String token = client.getAccessToken().getTokenValue();
        String email = oAuth2User.getAttribute("email");

        Users user = userRepository.findByEmail(email).orElseThrow();

        return jobService.syncJobs(token, user);
    }

    @GetMapping
    public List<JobApplication> getAll(@AuthenticationPrincipal OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");

        Users user = userRepository.findByEmail(email).orElseThrow();

        return user.getJobApplications();
    }
}
