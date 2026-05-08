package com.jobassistant.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EmailParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // RFC 2822 email date formats
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z",   Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z",         Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss zzz",       Locale.ENGLISH)
    );

    // "for <role> at/with/in/–" pattern
    private static final Pattern ROLE_FOR_PATTERN = Pattern.compile(
            "(?i)\\bfor\\s+(?:the\\s+|a\\s+|an\\s+)?(.+?)(?:\\s+(?:at|with|from|in|position|role)\\b|\\s*[-–|@]|$)"
    );

    // "at/with/from Company" pattern in subject
    private static final Pattern COMPANY_SUBJECT_PATTERN = Pattern.compile(
            "(?i)\\b(?:at|with|to)\\s+([A-Z][A-Za-z0-9&.'\\-]+(?:\\s+[A-Z][A-Za-z0-9&.'\\-]+){0,2})"
    );

    // "Company" at end after dash/pipe
    private static final Pattern COMPANY_TAIL_PATTERN = Pattern.compile(
            "[-–|]\\s*([A-Z][A-Za-z0-9&.'\\-]+(?:\\s+[A-Z][A-Za-z0-9&.'\\-]+)?)\\s*$"
    );

    private static final Set<String> NOISE_COMPANY_WORDS = Set.of(
            "your", "the", "our", "my", "new", "update", "applied", "application",
            "please", "thank", "you", "dear", "hi", "hello", "we", "us", "it",
            "this", "that", "position", "role", "team", "hr", "jobs", "recruiting"
    );

    private static final Set<String> GENERIC_EMAIL_DOMAINS = Set.of(
            "gmail", "yahoo", "outlook", "hotmail", "naukri", "linkedin", "indeed",
            "glassdoor", "google", "workday", "greenhouse", "lever", "ashby", "rippling",
            "smartrecruiters", "jobvite", "icims", "successfactors", "taleo"
    );

    public Map<String, String> parse(String emailJson) {
        Map<String, String> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(emailJson);
            JsonNode headers = root.path("payload").path("headers");

            for (JsonNode header : headers) {
                String name  = header.path("name").asText();
                String value = header.path("value").asText();

                switch (name.toLowerCase()) {
                    case "subject" -> result.put("subject", value);
                    case "from"    -> result.put("from", value);
                    case "date"    -> result.put("date", value);
                }
            }

            result.put("snippet",   root.path("snippet").asText());
            result.put("messageId", root.path("id").asText());

        } catch (Exception e) {
            log.error("Failed to parse email JSON: {}", e.getMessage());
        }
        return result;
    }

    public String extractSource(String from) {
        if (from == null || from.isBlank()) return "Direct";
        String lower = from.toLowerCase();
        if (lower.contains("linkedin"))                              return "LinkedIn";
        if (lower.contains("indeed"))                               return "Indeed";
        if (lower.contains("naukri"))                               return "Naukri";
        if (lower.contains("glassdoor"))                            return "Glassdoor";
        if (lower.contains("shine"))                                return "Shine";
        if (lower.contains("monster"))                              return "Monster";
        if (lower.contains("instahyre"))                            return "Instahyre";
        if (lower.contains("cutshort"))                             return "Cutshort";
        if (lower.contains("angellist") || lower.contains("wellfound")) return "AngelList";
        return "Direct";
    }

    /**
     * Extracts company from subject first, then falls back to the From field.
     */
    public String extractCompany(String subject, String from) {
        if (subject != null) {
            Matcher m = COMPANY_SUBJECT_PATTERN.matcher(subject);
            if (m.find()) {
                String candidate = m.group(1).trim();
                if (!isNoiseCompany(candidate)) return candidate;
            }

            Matcher m2 = COMPANY_TAIL_PATTERN.matcher(subject);
            if (m2.find()) {
                String candidate = m2.group(1).trim();
                if (!isNoiseCompany(candidate)) return candidate;
            }
        }
        return extractCompanyFromFrom(from);
    }

    /**
     * Extracts a clean role name from the subject line.
     * Tries "for <role> at/with" pattern first, then strips common noise prefixes.
     */
    public String extractRole(String subject) {
        if (subject == null || subject.isBlank()) return "Unknown Role";

        String clean = subject.replaceAll("(?i)^(re:|fwd:|fw:)\\s*", "").trim();

        Matcher m = ROLE_FOR_PATTERN.matcher(clean);
        if (m.find()) {
            String role = m.group(1).trim();
            if (role.length() > 2 && role.length() < 70) return capitalise(role);
        }

        // Strip leading action words to get at the role title
        String stripped = clean
                .replaceAll("(?i)^(your application to|your application for|application to|application for|application -|application:|interview for|interview -|interview:|offer for|offer:|rejection for|update on|regarding|congratulations on|thank you for applying to|thank you for applying|you applied for|you applied to)\\s*", "")
                .replaceAll("(?i)\\s+(at|with|from)\\s+[A-Za-z].*$", "")
                .trim();

        if (!stripped.isBlank() && stripped.length() > 2) {
            return capitalise(stripped.length() > 80 ? stripped.substring(0, 80) : stripped);
        }

        return capitalise(clean.length() > 80 ? clean.substring(0, 80) : clean);
    }

    /**
     * Parses the RFC 2822 Date header from the email.
     * Falls back to today if the header is missing or unparseable.
     */
    public LocalDate extractDate(String dateHeader) {
        if (dateHeader == null || dateHeader.isBlank()) return LocalDate.now();

        // Strip trailing parenthetical timezone like "(IST)" that breaks parsers
        String cleaned = dateHeader.replaceAll("\\s*\\([^)]*\\)", "").trim();

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return ZonedDateTime.parse(cleaned, fmt).toLocalDate();
            } catch (DateTimeParseException ignored) {}
        }

        log.warn("Could not parse email date header: {}", dateHeader);
        return LocalDate.now();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String extractCompanyFromFrom(String from) {
        if (from == null || from.isBlank()) return "Unknown";

        if (from.contains("<")) {
            String displayName = from.split("<")[0].trim()
                    .replaceAll("(?i)\\s*(recruiting|careers|hr|jobs|talent|team|hiring|recruitment|notifications?|alerts?|noreply|no.reply)\\b.*", "")
                    .trim();
            if (!displayName.isBlank() && !isNoiseCompany(displayName)) {
                String first = displayName.split("\\s+")[0];
                if (!first.isBlank()) return capitalise(first);
            }
        }

        // Last resort: extract company from email domain
        Pattern emailPat = Pattern.compile("[^@<\\s]+@([^.>\\s]+)");
        Matcher m = emailPat.matcher(from);
        if (m.find()) {
            String domain = m.group(1).toLowerCase();
            if (!GENERIC_EMAIL_DOMAINS.contains(domain)) return capitalise(domain);
        }

        return "Unknown";
    }

    private boolean isNoiseCompany(String word) {
        return word == null
                || word.length() < 2
                || NOISE_COMPANY_WORDS.contains(word.toLowerCase().split("\\s+")[0]);
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
