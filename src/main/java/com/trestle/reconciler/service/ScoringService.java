package com.trestle.reconciler.service;

import com.trestle.reconciler.config.MatchingConfig;
import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.FieldScore;
import com.trestle.reconciler.dto.MatchResult;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ScoringService {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

    @Autowired
    private MatchingConfig config;

    public MatchResult score(CandidatePair pair) {
        NormalizedPersonRecord a = pair.getRecordA();
        NormalizedPersonRecord b = pair.getRecordB();

        Map<String, FieldScore> breakdown = new LinkedHashMap<>();

        // Phone — exact match only (binary)
        double phoneScore = exactMatch(a.getNormalizedPhone(), b.getNormalizedPhone());
        breakdown.put("phone", new FieldScore(phoneScore, phoneNote(a.getNormalizedPhone(), b.getNormalizedPhone(), phoneScore)));

        // Name — Jaro-Winkler similarity (handles typos and spelling variants)
        String nameA = (a.getFirstName() + " " + a.getLastName()).trim();
        String nameB = (b.getFirstName() + " " + b.getLastName()).trim();
        double nameScore = scoreText(nameA, nameB);
        breakdown.put("name", new FieldScore(round(nameScore), nameNote(nameA, nameB, nameScore)));

        // DOB — graduated: exact=1.0, same year+month=0.5, same year=0.2
        double dobScore = scoreDob(a.getParsedDob(), b.getParsedDob());
        breakdown.put("dob", new FieldScore(dobScore, dobNote(a.getParsedDob(), b.getParsedDob(), dobScore)));

        // Email — exact=1.0, same domain=0.3, else=0.0
        double emailScore = scoreEmail(a.getNormalizedEmail(), b.getNormalizedEmail());
        breakdown.put("email", new FieldScore(emailScore, emailNote(a.getNormalizedEmail(), b.getNormalizedEmail(), emailScore)));

        // Address — Jaro-Winkler similarity
        double addressScore = scoreText(a.getNormalizedAddress(), b.getNormalizedAddress());
        breakdown.put("address", new FieldScore(round(addressScore), addressNote(a.getNormalizedAddress(), b.getNormalizedAddress(), addressScore)));

        double confidence = (config.getPhoneWeight()   * phoneScore)
                          + (config.getNameWeight()    * nameScore)
                          + (config.getDobWeight()     * dobScore)
                          + (config.getEmailWeight()   * emailScore)
                          + (config.getAddressWeight() * addressScore);

        MatchResult result = new MatchResult();
        result.setPairId(pair.getPairId());
        result.setConfidence(round(confidence));
        result.setLabel(assignLabel(confidence));
        result.setFieldBreakdown(breakdown);
        result.setSummary(buildSummary(breakdown, confidence));

        log.debug("Scored pair {} — confidence: {}, label: {}", pair.getPairId(), result.getConfidence(), result.getLabel());
        return result;
    }

    // Returns 1.0 for exact match, 0.0 for mismatch or null
    private double exactMatch(String a, String b) {
        if (a == null || b == null) return 0.0;
        return a.equals(b) ? 1.0 : 0.0;
    }

    // Jaro-Winkler similarity — returns 0.0 if either value is null
    private double scoreText(String a, String b) {
        if (a == null || b == null) return 0.0;
        return JARO_WINKLER.apply(a, b);
    }

    // Exact DOB = 1.0 · same year+month = 0.5 · same year = 0.2 · mismatch = 0.0
    private double scoreDob(LocalDate a, LocalDate b) {
        if (a == null || b == null) return 0.0;
        if (Objects.equals(a, b)) return 1.0;
        if (a.getYear() == b.getYear() && a.getMonth() == b.getMonth()) return 0.5;
        if (a.getYear() == b.getYear()) return 0.2;
        return 0.0;
    }

    // Exact email = 1.0 · same domain = 0.3 · no match = 0.0
    private double scoreEmail(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (a.equals(b)) return 1.0;
        String domainA = a.contains("@") ? a.substring(a.indexOf('@')) : "";
        String domainB = b.contains("@") ? b.substring(b.indexOf('@')) : "";
        if (!domainA.isEmpty() && domainA.equals(domainB)) return 0.3;
        return 0.0;
    }

    private String phoneNote(String a, String b, double score) {
        if (a == null || b == null) return "One or both phone numbers are missing";
        return score == 1.0 ? "Exact phone match: " + a
                : "Phone mismatch: " + a + " vs " + b;
    }

    private String nameNote(String a, String b, double score) {
        if (a == null || b == null) return "One or both names are missing";
        if (score >= 0.95) return "Names are identical or near-identical: \"" + a + "\" vs \"" + b + "\"";
        if (score >= 0.80) return "Names are very similar (score " + round(score) + "): \"" + a + "\" vs \"" + b + "\"";
        if (score >= 0.60) return "Names have moderate similarity (score " + round(score) + "): \"" + a + "\" vs \"" + b + "\"";
        return "Names do not match (score " + round(score) + "): \"" + a + "\" vs \"" + b + "\"";
    }

    private String dobNote(LocalDate a, LocalDate b, double score) {
        if (a == null || b == null) return "One or both dates of birth are missing";
        if (score == 1.0) return "Exact date of birth match: " + a;
        if (score == 0.5) return "Same year and month, different day: " + a + " vs " + b;
        if (score == 0.2) return "Same birth year only: " + a + " vs " + b;
        return "Date of birth mismatch: " + a + " vs " + b;
    }

    private String emailNote(String a, String b, double score) {
        if (a == null || b == null) return "One or both emails are missing";
        if (score == 1.0) return "Exact email match: " + a;
        if (score == 0.3) {
            String domain = a.contains("@") ? a.substring(a.indexOf('@')) : "";
            return "Same email domain (" + domain + ") but different username: " + a + " vs " + b;
        }
        return "Email mismatch: " + a + " vs " + b;
    }

    private String addressNote(String a, String b, double score) {
        if (a == null || b == null) return "One or both addresses are missing";
        if (score >= 0.95) return "Addresses are identical or near-identical";
        if (score >= 0.80) return "Addresses are very similar (score " + round(score) + "): \"" + a + "\" vs \"" + b + "\"";
        if (score >= 0.60) return "Addresses have moderate similarity (score " + round(score) + ")";
        return "Addresses do not match (score " + round(score) + "): \"" + a + "\" vs \"" + b + "\"";
    }

    private String assignLabel(double confidence) {
        if (confidence >= config.getHighConfidenceThreshold()) return "HIGH_CONFIDENCE";
        if (confidence >= config.getReviewRequiredThreshold()) return "REVIEW_REQUIRED";
        return "NO_MATCH";
    }

    private String buildSummary(Map<String, FieldScore> breakdown, double confidence) {
        List<String> strong = new ArrayList<>();
        List<String> weak   = new ArrayList<>();

        breakdown.forEach((field, fs) -> {
            if (fs.getScore() >= 0.8) strong.add(field);
            else weak.add(field);
        });

        StringBuilder sb = new StringBuilder();
        if (!strong.isEmpty()) {
            sb.append("Strong match on: ").append(String.join(", ", strong)).append(". ");
        }
        if (!weak.isEmpty()) {
            sb.append("Weak or no match on: ").append(String.join(", ", weak)).append(". ");
        }
        sb.append("Overall confidence: ").append(round(confidence))
          .append(" (").append(assignLabel(confidence)).append(").");
        return sb.toString();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
