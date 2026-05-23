package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.MatchResult;
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

    @Autowired
    private ScoringService scoringService;

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

        List<MatchResult> results = candidates.stream()
                .map(scoringService::score)
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .toList();

        int highConfidence = (int) results.stream().filter(r -> "HIGH_CONFIDENCE".equals(r.getLabel())).count();
        int reviewRequired = (int) results.stream().filter(r -> "REVIEW_REQUIRED".equals(r.getLabel())).count();
        int noMatch        = (int) results.stream().filter(r -> "NO_MATCH".equals(r.getLabel())).count();

        ReconcileResponse.Stats stats = new ReconcileResponse.Stats(
                candidates.size(), highConfidence, reviewRequired, noMatch,
                System.currentTimeMillis() - start);

        return new ReconcileResponse(results, stats);
    }
}
