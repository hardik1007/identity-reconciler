package com.trestle.reconciler.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class NormalizedPersonRecord {
    private String id;
    private String sourceId;          // "A" or "B" — which input file this came from
    private String firstName;
    private String lastName;
    private String normalizedPhone;   // digits only
    private String normalizedEmail;   // lowercase
    private String phoneticNameKey;   // phonetic encoding of full name
    private LocalDate parsedDob;      // parsed from raw dob string
    private String normalizedAddress; // lowercase
    private RawPersonRecord raw;      // original record preserved for explanation output
}
