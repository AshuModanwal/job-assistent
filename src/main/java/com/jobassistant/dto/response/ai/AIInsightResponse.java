package com.jobassistant.dto.response.ai;

import com.jobassistant.enums.InsightType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AIInsightResponse {

    private Long id;

    private String question;

    private String answer;

    private InsightType type;

    private LocalDateTime createdAt;
}