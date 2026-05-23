package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.FieldScore;
import com.trestle.reconciler.dto.MatchResult;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ScoringService {

    public MatchResult score(CandidatePair pair) {
        NormalizedPersonRecord a = pair.getRecordA();
        NormalizedPersonRecord b = pair.getRecordB();

        Map<String, FieldScore> breakdown = new LinkedHashMap<>();

        double phoneScore = exactMatch(a.getNormalizedPhone(), b.getNormalizedPhone());
        breakdown.put("phone", new FieldScore(phoneScore,
                a.getNormalizedPhone() + " vs " + b.getNormalizedPhone()));

        String nameA = (a.getFirstName() + " " + a.getLastName()).trim();
        String nameB = (b.getFirstName() + " " + b.getLastName()).trim();
        double nameScore = exactMatch(nameA, nameB);
        breakdown.put("name", new FieldScore(nameScore, nameA + " vs " + nameB));

        double dobScore = Objects.equals(a.getParsedDob(), b.getParsedDob()) ? 1.0 : 0.0;
        breakdown.put("dob", new FieldScore(dobScore,
                a.getParsedDob() + " vs " + b.getParsedDob()));

        double emailScore = exactMatch(a.getNormalizedEmail(), b.getNormalizedEmail());
        breakdown.put("email", new FieldScore(emailScore,
                a.getNormalizedEmail() + " vs " + b.getNormalizedEmail()));

        double addressScore = exactMatch(a.getNormalizedAddress(), b.getNormalizedAddress());
        breakdown.put("address", new FieldScore(addressScore,
                a.getNormalizedAddress() + " vs " + b.getNormalizedAddress()));

        double confidence = breakdown.values().stream()
                .mapToDouble(FieldScore::getScore)
                .average()
                .orElse(0.0);

        MatchResult result = new MatchResult();
        result.setPairId(pair.getPairId());
        result.setConfidence(Math.round(confidence * 100.0) / 100.0);
        result.setLabel(assignLabel(confidence));
        result.setFieldBreakdown(breakdown);
        result.setSummary(buildSummary(breakdown));
        return result;
    }

    private double exactMatch(String a, String b) {
        if (a == null || b == null) return 0.0;
        return a.equals(b) ? 1.0 : 0.0;
    }

    private String assignLabel(double confidence) {
        if (confidence >= 0.85) return "HIGH_CONFIDENCE";
        if (confidence >= 0.60) return "REVIEW_REQUIRED";
        return "NO_MATCH";
    }

    private String buildSummary(Map<String, FieldScore> breakdown) {
        long matched = breakdown.values().stream()
                .filter(f -> f.getScore() == 1.0)
                .count();
        return matched + " of " + breakdown.size() + " fields matched exactly";
    }
}
