package com.trestle.reconciler.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class NormalizedPersonRecord {
    private String id;
    private String sourceId;          // "A" or "B" — which input file this came from
    private String firstName;
    private String lastName;
    private String normalizedPhone;   // digits only or E.164 format
    private String normalizedEmail;   // lowercase, +alias stripped
    private String phoneticNameKey;   // DoubleMetaphone of full name (added in step 7)
    private LocalDate parsedDob;      // parsed from raw dob string
    private String normalizedAddress; // lowercase, abbreviations expanded
    private RawPersonRecord raw;      // original record — preserved for explanation output
}
