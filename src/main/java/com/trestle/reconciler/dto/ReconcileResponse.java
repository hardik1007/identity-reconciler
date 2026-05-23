package com.trestle.reconciler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReconcileResponse {
    private List<String> matches;
    private long processingTimeMs;
}
