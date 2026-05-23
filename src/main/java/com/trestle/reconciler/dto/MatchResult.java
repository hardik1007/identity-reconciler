package com.trestle.reconciler.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MatchResult {
    private String pairId;
    private double confidence;
    private String label;
    private Map<String, FieldScore> fieldBreakdown;
    private String summary;
}
