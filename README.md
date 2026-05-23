# Identity Reconciler

A Spring Boot service that compares two sets of person records and identifies matching individuals across them — even when names are spelled differently, phone numbers are formatted inconsistently, or fields are missing.

---

## How It Works

```
POST /api/v1/reconcile/upload
        │
        ▼
  ┌─────────────┐
  │   Parse     │  CSV → RawPersonRecord (line by line, streaming)
  └──────┬──────┘
         │
         ▼
  ┌─────────────┐
  │  Normalize  │  lowercase · digit-strip phone · parse DOB · phonetic name key
  └──────┬──────┘
         │
         ▼
  ┌─────────────┐
  │   Block     │  Generate keys → inverted index → candidate pairs
  └──────┬──────┘
         │
         ▼
  ┌─────────────┐
  │    Score    │  Exact match per field → weighted confidence → label
  └──────┬──────┘
         │
         ▼
  ReconcileResponse  (matches + stats)
```

---

## API

### `POST /api/v1/reconcile/upload`

Upload two CSV files for comparison.

**Request** — `multipart/form-data`

| Field | Type | Description |
|-------|------|-------------|
| `sourceA` | file | First set of person records (CSV) |
| `sourceB` | file | Second set of person records (CSV) |

**CSV format** (header row required):
```
id,firstName,lastName,fullName,phone,email,dob,address
```

**Example curl:**
```bash
curl -X POST http://localhost:8080/api/v1/reconcile/upload \
  -F "sourceA=@source-a.csv" \
  -F "sourceB=@source-b.csv"
```

**Response:**
```json
{
  "matches": [
    {
      "pairId": "A1|B1",
      "confidence": 0.85,
      "label": "HIGH_CONFIDENCE",
      "fieldBreakdown": {
        "phone":   { "score": 1.0, "note": "919876543210 vs 919876543210" },
        "name":    { "score": 1.0, "note": "john smith vs john smith" },
        "dob":     { "score": 1.0, "note": "1985-03-12 vs 1985-03-12" },
        "email":   { "score": 0.0, "note": "john@gmail.com vs jane@gmail.com" },
        "address": { "score": 0.0, "note": "123 main st vs 456 oak ave" }
      },
      "summary": "3 of 5 fields matched exactly"
    }
  ],
  "stats": {
    "totalCandidates": 12,
    "highConfidence": 3,
    "reviewRequired": 5,
    "noMatch": 4,
    "processingTimeMs": 142
  }
}
```

**Labels:**

| Label | Meaning |
|-------|---------|
| `HIGH_CONFIDENCE` | confidence ≥ 0.85 — strong match |
| `REVIEW_REQUIRED` | confidence ≥ 0.60 — possible match, needs human review |
| `NO_MATCH` | confidence < 0.60 — unlikely to be the same person |

---

---

## Configuration

All weights and thresholds are in `application.properties` — no code change needed to tune them.

```properties
# Field weights (must sum to 1.0)
matching.weight.phone=0.35
matching.weight.name=0.30
matching.weight.dob=0.20
matching.weight.email=0.10
matching.weight.address=0.05

# Label thresholds
matching.threshold.high-confidence=0.85
matching.threshold.review-required=0.60
```

---

## Normalization

| Field | What happens |
|-------|-------------|
| **Phone** | All non-digit characters stripped — `+91-9876543210` → `919876543210` |
| **Email** | Lowercased and trimmed |
| **Name** | Lowercased · `"Smith, John"` flipped to `first=john last=smith` · phonetic key via DoubleMetaphone |
| **DOB** | Parsed to `LocalDate` — supports `yyyy-MM-dd`, `MM/dd/yyyy`, `dd/MM/yyyy` |
| **Address** | Lowercased and trimmed |

---

## Blocking

Records are only compared if they share at least one blocking key. This avoids comparing every record against every other record.

| Key | Example | Purpose |
|-----|---------|---------|
| `phone:` | `phone:919876543210` | Exact phone match |
| `email:` | `email:john@gmail.com` | Exact email match |
| `phonetic:` | `phonetic:JNSM` | Name variants — Jon / John, Smith / Smyth |
| `dob_name:` | `dob_name:1985_JNSM` | Birth year + phonetic name — reduces false positives |

---

## Scaling to 200GB

The current design is synchronous and in-memory — it works well for thousands of records but would need the following changes for large-scale or streaming inputs.

### Kafka — streaming ingestion and decoupled pipeline

- Instead of uploading files directly, stream each record as a Kafka message to an `identity.raw-records` topic
- Each pipeline stage (normalise, block, score) becomes an independent Kafka consumer group
- The API returns a `jobId` immediately (202 Accepted) and the client polls for results
- Scoring consumers are stateless — they can scale horizontally without any code change

### Redis — distributed blocking index

- The current in-memory `Map<String, List<>>` inverted index is replaced with Redis Sets
- Each blocking key maps to a Redis Set of record IDs: `SADD block:{jobId}:{key} recordId`
- When all source B records are indexed, a pair-emitter scans the Redis index and publishes candidate pairs to Kafka
- Keys are given a TTL (e.g. 48h) and explicitly cleaned up when the job completes

### OpenSearch — fuzzy name and address matching at scale

- Instead of running Jaro-Winkler on every candidate pair, index normalised records into OpenSearch
- Use `fuzzy` or `match` queries on name and address fields to find candidates directly — replacing the blocking step entirely for text fields
- OpenSearch handles typos, phonetic variants, and abbreviation differences natively
- Results are scored by OpenSearch relevance and can be combined with exact field scores for the final confidence

### What stays the same

`ScoringService`, `RecordNormalizerService`, and `MatchingConfig` require no changes — the scoring logic is stateless and works the same whether called from an HTTP thread or a Kafka consumer.

---

## Running Locally

**Prerequisites:** Java 21, Maven

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

---

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Lombok
- Apache Commons Text — text similarity
- springdoc OpenAPI — Swagger UI


## Appendix: AI Usage

This service was built incrementally with Claude (claude.ai) as a coding assistant. Here is an honest account of where AI was used, what was accepted, and what was changed.

### What AI generated

- Initial project structure and package layout (controller / service / dto / config)
- Boilerplate for all service classes with method signatures and field mappings
- The blocking key strategy — four keys (phone, email, phonetic, dob+phonetic)
- The graduated DOB scoring tiers (exact=1.0, yr+month=0.5, yr=0.2)
- The `emailNote`, `phoneNote`, `nameNote`, `dobNote`, `addressNote` methods in `ScoringService`
- All unit test scaffolding and test cases for normalisation and scoring
- The multi-stage Dockerfile and GitHub Actions workflow
- Generating test data sample csv for API testing.
- Creating README.md file content with proper format

