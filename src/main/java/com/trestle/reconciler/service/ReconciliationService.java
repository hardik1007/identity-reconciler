package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.NormalizedPersonRecord;
import com.trestle.reconciler.dto.RawPersonRecord;
import com.trestle.reconciler.dto.ReconcileResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class ReconciliationService {

    private final RecordParserService parser;
    private final RecordNormalizerService normalizer;

    public ReconciliationService(RecordParserService parser,
                                 RecordNormalizerService normalizer) {
        this.parser = parser;
        this.normalizer = normalizer;
    }

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

        long processingTimeMs = System.currentTimeMillis() - start;
        return new ReconcileResponse(List.of(), processingTimeMs);
    }
}
