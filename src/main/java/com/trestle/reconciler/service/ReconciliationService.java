package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import com.trestle.reconciler.dto.RawPersonRecord;
import com.trestle.reconciler.dto.ReconcileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

@Service
public class ReconciliationService {

    @Autowired
    private RecordParserService parser;

    @Autowired
    private RecordNormalizerService normalizer;

    @Autowired
    private CandidatePairFinderService pairFinder;

    public ReconcileResponse reconcile(MultipartFile sourceA, MultipartFile sourceB) throws Exception {
        long start = System.currentTimeMillis();

        List<RawPersonRecord> rawA = parser.parse(new BufferedReader(new InputStreamReader(sourceA.getInputStream())));
        List<RawPersonRecord> rawB = parser.parse(new BufferedReader(new InputStreamReader(sourceB.getInputStream())));

        List<NormalizedPersonRecord> normA = rawA.stream()
                .map(r -> normalizer.normalize(r, "A"))
                .toList();
        List<NormalizedPersonRecord> normB = rawB.stream()
                .map(r -> normalizer.normalize(r, "B"))
                .toList();

        Set<CandidatePair> candidates = pairFinder.findCandidates(normA, normB);

        long processingTimeMs = System.currentTimeMillis() - start;
        return new ReconcileResponse(List.of(), processingTimeMs);
    }
}
