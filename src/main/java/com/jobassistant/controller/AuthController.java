package com.jobassistant.controller;

import com.jobassistant.entity.Users;
import com.jobassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    // ✅ Get or create logged-in user
    @GetMapping("/me")
    public Users getCurrentUser(@AuthenticationPrincipal OAuth2User oAuth2User) {

        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        Users.builder()
                                .email(email)
                                .googleId(googleId)
                                .build()
                ));
    }
}