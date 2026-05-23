package com.trestle.reconciler.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(of = "pairId")
public class CandidatePair {
    private final String pairId;                  // canonical: min(idA, idB) + "|" + max(idA, idB)
    private final NormalizedPersonRecord recordA;
    private final NormalizedPersonRecord recordB;

    // Ensures the same two records always produce the same pairId regardless of order
    public static CandidatePair of(NormalizedPersonRecord a, NormalizedPersonRecord b) {
        String idA = a.getId();
        String idB = b.getId();
        String pairId = idA.compareTo(idB) <= 0 ? idA + "|" + idB : idB + "|" + idA;
        return new CandidatePair(pairId, a, b);
    }
}
