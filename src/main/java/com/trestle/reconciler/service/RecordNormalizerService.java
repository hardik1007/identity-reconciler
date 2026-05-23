package com.trestle.reconciler.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import com.trestle.reconciler.dto.RawPersonRecord;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RecordNormalizerService {

    private static final DoubleMetaphone METAPHONE = new DoubleMetaphone();
    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final Pattern PLUS_ALIAS = Pattern.compile("\\+[^@]+");

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

    // Numbers with a + prefix are parsed via libphonenumber to E.164.
    // All others fall back to digit-only stripping.
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        if (phone.trim().startsWith("+")) {
            try {
                Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(phone.trim(), null);
                if (PHONE_UTIL.isValidNumber(parsed)) {
                    return PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
                }
            } catch (NumberParseException ignored) {}
        }
        String digits = phone.replaceAll("[^\\d]", "");
        return digits.isEmpty() ? null : digits;
    }

    // Lowercases and strips the +alias part — john+work@gmail.com → john@gmail.com
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        email = email.trim().toLowerCase();
        int atIdx = email.indexOf('@');
        if (atIdx > 0) {
            String local  = PLUS_ALIAS.matcher(email.substring(0, atIdx)).replaceAll("");
            String domain = email.substring(atIdx);
            return local + domain;
        }
        return email;
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

    // Lowercases and expands common address abbreviations
    private String normalizeAddress(String address) {
        if (address == null || address.isBlank()) return null;
        return address.trim().toLowerCase()
                .replace(" st ",  " street ")
                .replace(" st,",  " street,")
                .replace(" ave ", " avenue ")
                .replace(" ave,", " avenue,")
                .replace(" rd ",  " road ")
                .replace(" rd,",  " road,")
                .replace(" blvd ", " boulevard ")
                .replace(" dr ",  " drive ")
                .replace(" ln ",  " lane ")
                .replace(" ct ",  " court ");
    }
}
