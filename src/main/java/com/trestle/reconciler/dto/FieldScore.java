package com.trestle.reconciler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldScore {
    private double score; // 0.0 to 1.0
    private String note;  // human-readable explanation of this field's score
}
