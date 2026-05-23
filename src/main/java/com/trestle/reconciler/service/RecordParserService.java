package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.RawPersonRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecordParserService {

    // Accepts a BufferedReader so the caller controls the stream source.
    // Processes line by line — never loads the full file into memory.
    public List<RawPersonRecord> parse(BufferedReader reader) throws IOException {
        List<RawPersonRecord> records = new ArrayList<>();
        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            if (firstLine) { firstLine = false; continue; } // skip header
            if (line.isBlank()) continue;

            String[] parts = line.split(",", -1);
            RawPersonRecord r = new RawPersonRecord();
            r.setId(get(parts, 0));
            r.setFirstName(get(parts, 1));
            r.setLastName(get(parts, 2));
            r.setFullName(get(parts, 3));
            r.setPhone(get(parts, 4));
            r.setEmail(get(parts, 5));
            r.setDob(get(parts, 6));
            r.setAddress(get(parts, 7));
            records.add(r);
        }

        return records;
    }

    // Returns null for empty or missing fields so downstream code can distinguish missing vs blank
    private String get(String[] parts, int index) {
        if (index >= parts.length) return null;
        String value = parts[index].trim();
        return value.isEmpty() ? null : value;
    }
}
