package com.trestle.reconciler.service;

import com.trestle.reconciler.dto.NormalizedPersonRecord;
import com.trestle.reconciler.dto.RawPersonRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordNormalizerServiceTest {

    private RecordNormalizerService normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new RecordNormalizerService();
    }

    // ── Phone ────────────────────────────────────────────────────────────────

    @Test
    void phone_withCountryCode_normalisedToE164() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setPhone("+91-9876543210")), "A");
        assertThat(result.getNormalizedPhone()).isEqualTo("+919876543210");
    }

    @Test
    void phone_withoutPlus_fallsBackToDigitsOnly() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setPhone("98765-43210")), "A");
        assertThat(result.getNormalizedPhone()).isEqualTo("9876543210");
    }

    @Test
    void phone_null_returnsNull() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setPhone(null)), "A");
        assertThat(result.getNormalizedPhone()).isNull();
    }

    // ── Email ────────────────────────────────────────────────────────────────

    @Test
    void email_aliasStripped() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setEmail("john+work@gmail.com")), "A");
        assertThat(result.getNormalizedEmail()).isEqualTo("john@gmail.com");
    }

    @Test
    void email_uppercaseLowercased() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setEmail("JOHN@GMAIL.COM")), "A");
        assertThat(result.getNormalizedEmail()).isEqualTo("john@gmail.com");
    }

    @Test
    void email_null_returnsNull() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setEmail(null)), "A");
        assertThat(result.getNormalizedEmail()).isNull();
    }

    // ── Name ─────────────────────────────────────────────────────────────────

    @Test
    void name_lastFirstFormat_flipped() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setFullName("Smith, John")), "A");
        assertThat(result.getFirstName()).isEqualTo("john");
        assertThat(result.getLastName()).isEqualTo("smith");
    }

    @Test
    void name_firstLastFormat_splitCorrectly() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setFullName("John Smith")), "A");
        assertThat(result.getFirstName()).isEqualTo("john");
        assertThat(result.getLastName()).isEqualTo("smith");
    }

    @Test
    void name_separateFields_usedWhenFullNameAbsent() {
        RawPersonRecord r = new RawPersonRecord();
        r.setId("1");
        r.setFirstName("Jane");
        r.setLastName("Doe");
        NormalizedPersonRecord result = normalizer.normalize(r, "A");
        assertThat(result.getFirstName()).isEqualTo("jane");
        assertThat(result.getLastName()).isEqualTo("doe");
    }

    @Test
    void name_jonAndJohn_samePhoneticKey() {
        NormalizedPersonRecord jon  = normalizer.normalize(raw(r -> r.setFullName("Jon Smith")),  "A");
        NormalizedPersonRecord john = normalizer.normalize(raw(r -> r.setFullName("John Smith")), "B");
        assertThat(jon.getPhoneticNameKey()).isEqualTo(john.getPhoneticNameKey());
    }

    // ── DOB ──────────────────────────────────────────────────────────────────

    @Test
    void dob_isoFormat_parsed() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setDob("1985-03-12")), "A");
        assertThat(result.getParsedDob()).isNotNull();
        assertThat(result.getParsedDob().getYear()).isEqualTo(1985);
        assertThat(result.getParsedDob().getMonthValue()).isEqualTo(3);
        assertThat(result.getParsedDob().getDayOfMonth()).isEqualTo(12);
    }

    @Test
    void dob_usFormat_parsed() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setDob("03/12/1985")), "A");
        assertThat(result.getParsedDob()).isNotNull();
        assertThat(result.getParsedDob().getYear()).isEqualTo(1985);
    }

    @Test
    void dob_unparseable_returnsNull() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setDob("not-a-date")), "A");
        assertThat(result.getParsedDob()).isNull();
    }

    // ── Address ──────────────────────────────────────────────────────────────

    @Test
    void address_stAbbreviationExpanded() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setAddress("123 Main St, City")), "A");
        assertThat(result.getNormalizedAddress()).contains("street");
    }

    @Test
    void address_aveAbbreviationExpanded() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setAddress("456 Oak Ave, City")), "A");
        assertThat(result.getNormalizedAddress()).contains("avenue");
    }

    @Test
    void address_lowercased() {
        NormalizedPersonRecord result = normalizer.normalize(raw(r -> r.setAddress("789 PINE ROAD")), "A");
        assertThat(result.getNormalizedAddress()).isEqualTo("789 pine road");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RawPersonRecord raw(java.util.function.Consumer<RawPersonRecord> setup) {
        RawPersonRecord r = new RawPersonRecord();
        r.setId("test-id");
        setup.accept(r);
        return r;
    }
}
