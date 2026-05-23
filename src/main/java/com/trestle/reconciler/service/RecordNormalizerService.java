package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.NormalizedPersonRecord;
import com.trestle.reconciler.dto.RawPersonRecord;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RecordNormalizerService {

    private static final DoubleMetaphone METAPHONE = new DoubleMetaphone();

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    public NormalizedPersonRecord normalize(RawPersonRecord raw, String sourceId) {
        NormalizedPersonRecord n = new NormalizedPersonRecord();
        n.setId(raw.getId());
        n.setSourceId(sourceId);
        n.setRaw(raw);

        String[] names = extractNames(raw);
        n.setFirstName(names[0]);
        n.setLastName(names[1]);
        n.setPhoneticNameKey(METAPHONE.doubleMetaphone(names[0] + " " + names[1]));

        n.setNormalizedPhone(normalizePhone(raw.getPhone()));
        n.setNormalizedEmail(normalizeEmail(raw.getEmail()));
        n.setParsedDob(parseDob(raw.getDob()));
        n.setNormalizedAddress(normalizeAddress(raw.getAddress()));

        return n;
    }

    // Detects "Last, First" format and flips it; otherwise splits on first space
    private String[] extractNames(RawPersonRecord raw) {
        String first = "";
        String last = "";

        String full = raw.getFullName() != null ? raw.getFullName().trim() : null;
        if (full != null && !full.isBlank()) {
            if (full.contains(",")) {
                String[] parts = full.split(",", 2);
                last  = parts[0].trim().toLowerCase();
                first = parts[1].trim().toLowerCase();
            } else {
                String[] parts = full.split("\\s+", 2);
                first = parts[0].toLowerCase();
                last  = parts.length > 1 ? parts[1].toLowerCase() : "";
            }
        } else {
            first = raw.getFirstName() != null ? raw.getFirstName().trim().toLowerCase() : "";
            last  = raw.getLastName()  != null ? raw.getLastName().trim().toLowerCase()  : "";
        }

        return new String[]{first, last};
    }

    // Strips all non-digit characters — e.g. +91-9876543210 → 919876543210
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^\\d]", "");
        return digits.isEmpty() ? null : digits;
    }

    // Lowercases and trims the email address
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase();
    }

    // Tries multiple date formats; returns null if none match
    private LocalDate parseDob(String dob) {
        if (dob == null || dob.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dob.trim(), fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // Lowercases and trims the address
    private String normalizeAddress(String address) {
        if (address == null || address.isBlank()) return null;
        return address.trim().toLowerCase();
    }
}
