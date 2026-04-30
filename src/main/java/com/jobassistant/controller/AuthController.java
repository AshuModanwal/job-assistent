package com.jobassistant.controller;

import com.jobassistant.entity.Users;
import com.jobassistant.repository.UserRepository;
import com.jobassistant.service.GmailService;
import com.jobassistant.util.HelperMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @Autowired
    private GmailService gmailService;

    @Autowired
    private HelperMethods helperMethods;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public Users getUser(@AuthenticationPrincipal OAuth2User oAuth2User) {

        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        Users user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Users newUser = new Users();
                    newUser.setEmail(email);
                    newUser.setGoogleId(googleId);
                    return userRepository.save(newUser);
                });

        return user;
    }


    @GetMapping("/token")
    public String getToken(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        return client.getAccessToken().getTokenValue();
    }



    @GetMapping("/emails")
    public String getEmails(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        String token = client.getAccessToken().getTokenValue();
        return gmailService.fetchEmails(token);
    }

    @GetMapping("/email-detail")
    public String getEmailDetail(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        String token = client.getAccessToken().getTokenValue();

        // Replace with one ID from your response
        String messageId = "19dd6ca7b0c62268";

        String emailJson = gmailService.getEmailDetails(token, messageId);
        helperMethods.parseEmail(emailJson);
        return "DONE";

    }
}
