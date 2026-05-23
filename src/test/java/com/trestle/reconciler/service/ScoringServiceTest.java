package com.trestle.reconciler.service;

import com.trestle.reconciler.config.MatchingConfig;
import com.trestle.reconciler.dto.CandidatePair;
import com.trestle.reconciler.dto.MatchResult;
import com.trestle.reconciler.dto.NormalizedPersonRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScoringServiceTest {

    @Mock
    private MatchingConfig config;

    @InjectMocks
    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        when(config.getPhoneWeight()).thenReturn(0.35);
        when(config.getNameWeight()).thenReturn(0.30);
        when(config.getDobWeight()).thenReturn(0.20);
        when(config.getEmailWeight()).thenReturn(0.10);
        when(config.getAddressWeight()).thenReturn(0.05);
        when(config.getHighConfidenceThreshold()).thenReturn(0.85);
        when(config.getReviewRequiredThreshold()).thenReturn(0.60);
    }

    // ── Label assignment ─────────────────────────────────────────────────────

    @Test
    void allFieldsMatch_highConfidence() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", "+919876543210", "john@gmail.com", "1985-03-12", "123 main street"),
                record("B1", "john", "smith", "+919876543210", "john@gmail.com", "1985-03-12", "123 main street")
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getLabel()).isEqualTo("HIGH_CONFIDENCE");
        assertThat(result.getConfidence()).isEqualTo(1.0);
    }

    @Test
    void completelyDifferentRecords_noMatch() {
        CandidatePair pair = pair(
                record("A1", "john",  "smith", "+910000000001", "john@gmail.com",  "1985-03-12", "123 main street"),
                record("B1", "zhang", "wei",   "+910000000002", "zhang@outlook.com", "1960-01-01", "999 other place")
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getLabel()).isEqualTo("NO_MATCH");
    }

    @Test
    void phoneAndDobMatch_othersDiffer_reviewRequired() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", "+919876543210", "john@gmail.com", "1985-03-12", "123 main street"),
                record("B1", "bob",  "brown", "+919876543210", "bob@yahoo.com",  "1985-03-12", "456 other ave")
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getLabel()).isEqualTo("REVIEW_REQUIRED");
    }

    // ── Per-field scores ─────────────────────────────────────────────────────

    @Test
    void phoneExactMatch_fieldScoreIsOne() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", "+919876543210", null, null, null),
                record("B1", "john", "smith", "+919876543210", null, null, null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("phone").getScore()).isEqualTo(1.0);
    }

    @Test
    void phoneMismatch_fieldScoreIsZero() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", "+910000000001", null, null, null),
                record("B1", "john", "smith", "+910000000002", null, null, null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("phone").getScore()).isEqualTo(0.0);
    }

    @Test
    void emailExactMatch_fieldScoreIsOne() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, "john@gmail.com", null, null),
                record("B1", "john", "smith", null, "john@gmail.com", null, null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("email").getScore()).isEqualTo(1.0);
    }

    @Test
    void emailSameDomain_fieldScoreIsPointThree() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, "john@gmail.com",   null, null),
                record("B1", "john", "smith", null, "robert@gmail.com", null, null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("email").getScore()).isEqualTo(0.3);
    }

    @Test
    void emailDifferentDomain_fieldScoreIsZero() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, "john@gmail.com", null, null),
                record("B1", "john", "smith", null, "john@yahoo.com", null, null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("email").getScore()).isEqualTo(0.0);
    }

    // ── DOB graduated scoring ────────────────────────────────────────────────

    @Test
    void dobExactMatch_scoreIsOne() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, null, "1985-03-12", null),
                record("B1", "john", "smith", null, null, "1985-03-12", null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("dob").getScore()).isEqualTo(1.0);
    }

    @Test
    void dobSameYearAndMonth_scoreIsPointFive() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, null, "1985-03-12", null),
                record("B1", "john", "smith", null, null, "1985-03-25", null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("dob").getScore()).isEqualTo(0.5);
    }

    @Test
    void dobSameYearOnly_scoreIsPointTwo() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, null, "1985-03-12", null),
                record("B1", "john", "smith", null, null, "1985-09-01", null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("dob").getScore()).isEqualTo(0.2);
    }

    @Test
    void dobMismatch_scoreIsZero() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, null, "1985-03-12", null),
                record("B1", "john", "smith", null, null, "2000-01-01", null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("dob").getScore()).isEqualTo(0.0);
    }

    // ── Name Jaro-Winkler ────────────────────────────────────────────────────

    @Test
    void identicalNames_scoreIsOne() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", null, null, null, null),
                record("B1", "john", "smith", null, null, null, null)
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getFieldBreakdown().get("name").getScore()).isEqualTo(1.0);
    }

    @Test
    void similarSpelling_nameScoreHighButNotOne() {
        CandidatePair pair = pair(
                record("A1", "jon",  "smith", null, null, null, null),
                record("B1", "john", "smith", null, null, null, null)
        );
        MatchResult result = scoringService.score(pair);
        double nameScore = result.getFieldBreakdown().get("name").getScore();
        assertThat(nameScore).isGreaterThan(0.80).isLessThan(1.0);
    }

    // ── Response structure ───────────────────────────────────────────────────

    @Test
    void result_alwaysHasPairId_summary_allFieldsInBreakdown() {
        CandidatePair pair = pair(
                record("A1", "john", "smith", "+919876543210", "john@gmail.com", "1985-03-12", "123 main street"),
                record("B1", "john", "smith", "+919876543210", "john@gmail.com", "1985-03-12", "123 main street")
        );
        MatchResult result = scoringService.score(pair);
        assertThat(result.getPairId()).isNotBlank();
        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getFieldBreakdown()).containsKeys("phone", "name", "dob", "email", "address");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NormalizedPersonRecord record(String id, String firstName, String lastName,
                                          String phone, String email, String dob, String address) {
        NormalizedPersonRecord r = new NormalizedPersonRecord();
        r.setId(id);
        r.setFirstName(firstName);
        r.setLastName(lastName);
        r.setNormalizedPhone(phone);
        r.setNormalizedEmail(email);
        r.setParsedDob(dob != null ? LocalDate.parse(dob) : null);
        r.setNormalizedAddress(address);
        return r;
    }

    private CandidatePair pair(NormalizedPersonRecord a, NormalizedPersonRecord b) {
        return CandidatePair.of(a, b);
    }
}
