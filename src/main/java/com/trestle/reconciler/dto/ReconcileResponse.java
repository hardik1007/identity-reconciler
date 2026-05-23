package com.trestle.reconciler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReconcileResponse {
    private List<MatchResult> matches;
    private Stats stats;

    @Data
    @AllArgsConstructor
    public static class Stats {
        private int totalCandidates;
        private int highConfidence;
        private int reviewRequired;
        private int noMatch;
        private long processingTimeMs;
    }
}
