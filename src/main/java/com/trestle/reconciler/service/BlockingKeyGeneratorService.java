package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.NormalizedPersonRecord;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class BlockingKeyGeneratorService {

    // Generates a set of blocking keys for a record.
    // Two records are compared only if they share at least one key.
    public Set<String> generateKeys(NormalizedPersonRecord record) {
        Set<String> keys = new HashSet<>();

        // Exact phone match — strong signal
        if (record.getNormalizedPhone() != null) {
            keys.add("phone:" + record.getNormalizedPhone());
        }

        // Exact email match
        if (record.getNormalizedEmail() != null) {
            keys.add("email:" + record.getNormalizedEmail());
        }

        // Phonetic name — catches spelling variants like Jon / John, Smith / Smyth
        if (record.getPhoneticNameKey() != null && !record.getPhoneticNameKey().isBlank()) {
            keys.add("phonetic:" + record.getPhoneticNameKey());
        }

        // Composite: birth year + phonetic name — reduces false positives from phonetic-only matches
        if (record.getParsedDob() != null && record.getPhoneticNameKey() != null) {
            keys.add("dob_name:" + record.getParsedDob().getYear() + "_" + record.getPhoneticNameKey());
        }

        return keys;
    }
}
