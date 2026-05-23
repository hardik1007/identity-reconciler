package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CandidatePairFinderService {

    @Autowired
    private BlockingKeyGeneratorService keyGenerator;

    // Builds an inverted index from source B, then looks up each source A record's
    // blocking keys to find candidate pairs. Uses canonical pairId to deduplicate
    // pairs that match on more than one blocking key.
    public Set<CandidatePair> findCandidates(List<NormalizedPersonRecord> sourceA,
                                              List<NormalizedPersonRecord> sourceB) {

        // Inverted index: blockingKey → records from source B sharing that key
        Map<String, List<NormalizedPersonRecord>> indexB = new HashMap<>();
        for (NormalizedPersonRecord record : sourceB) {
            for (String key : keyGenerator.generateKeys(record)) {
                indexB.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        }

        // For each source A record, find matching source B records via shared blocking keys
        Set<CandidatePair> pairs = new HashSet<>();
        for (NormalizedPersonRecord recordA : sourceA) {
            for (String key : keyGenerator.generateKeys(recordA)) {
                List<NormalizedPersonRecord> matches = indexB.getOrDefault(key, Collections.emptyList());
                for (NormalizedPersonRecord recordB : matches) {
                    pairs.add(CandidatePair.of(recordA, recordB));
                }
            }
        }

        return pairs;
    }
}
