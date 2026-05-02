package com.jobassistant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;

    private String role;

    private String source;

    private LocalDate appliedDate;

    private String emailId;

    private Double score;

    private String feedback;

    // ✅ NEW FIELD (CRITICAL)
    private String status; // APPLIED, INTERVIEW, NO_RESPONSE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;
}