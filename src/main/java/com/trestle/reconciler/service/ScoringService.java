package com.trestle.reconciler.service;

import com.trestle.reconciler.config.MatchingConfig;
import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.FieldScore;
import com.trestle.reconciler.dto.MatchResult;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
        breakdown.put("phone", new FieldScore(phoneScore,
                a.getNormalizedPhone() + " vs " + b.getNormalizedPhone()));

        // Name — Jaro-Winkler similarity (handles typos and spelling variants)
        String nameA = (a.getFirstName() + " " + a.getLastName()).trim();
        String nameB = (b.getFirstName() + " " + b.getLastName()).trim();
        double nameScore = scoreText(nameA, nameB);
        breakdown.put("name", new FieldScore(round(nameScore), nameA + " vs " + nameB));

        // DOB — graduated: exact=1.0, same year+month=0.5, same year=0.2
        double dobScore = scoreDob(a.getParsedDob(), b.getParsedDob());
        breakdown.put("dob", new FieldScore(dobScore,
                a.getParsedDob() + " vs " + b.getParsedDob()));

        // Email — exact=1.0, same domain=0.3, else=0.0
        double emailScore = scoreEmail(a.getNormalizedEmail(), b.getNormalizedEmail());
        breakdown.put("email", new FieldScore(emailScore,
                a.getNormalizedEmail() + " vs " + b.getNormalizedEmail()));

        // Address — Jaro-Winkler similarity
        double addressScore = scoreText(a.getNormalizedAddress(), b.getNormalizedAddress());
        breakdown.put("address", new FieldScore(round(addressScore),
                a.getNormalizedAddress() + " vs " + b.getNormalizedAddress()));

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

    private String assignLabel(double confidence) {
        if (confidence >= config.getHighConfidenceThreshold()) return "HIGH_CONFIDENCE";
        if (confidence >= config.getReviewRequiredThreshold()) return "REVIEW_REQUIRED";
        return "NO_MATCH";
    }

    private String buildSummary(Map<String, FieldScore> breakdown, double confidence) {
        long strongMatches = breakdown.values().stream()
                .filter(f -> f.getScore() >= 0.8)
                .count();
        return strongMatches + " of " + breakdown.size()
                + " fields strongly matched — confidence " + round(confidence);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
