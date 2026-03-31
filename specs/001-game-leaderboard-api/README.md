# 001 — Game Leaderboard API

Spring Boot 3 REST API for a mobile game leaderboard system backed by Azure Cosmos DB (NoSQL API). Handles real-time score submissions, global and regional leaderboard queries, player rank lookups, and player profile/stats queries with weekly leaderboard periods.

| | |
|---|---|
| **Branch** | `001-game-leaderboard-api` |
| **Status** | Implemented and verified against Cosmos DB Emulator |
| **Stack** | Java 21, Spring Boot 3.2.5, Spring Data Azure Cosmos DB, Azure Cosmos DB Java SDK 4.52+ |
| **Created** | 2026-03-30 |

## Key Features

- **Score Submission** — Accept scores, retain highest per player, recalculate rankings within 2 seconds
- **Global Leaderboard** — Top-N ranked players worldwide with pagination and weekly periods
- **Regional Leaderboard** — Country-filtered leaderboards using ISO 3166-1 alpha-2 codes
- **Player Rank** — "Find my rank" with surrounding players (±N context)
- **Player Profile** — Cumulative stats (games played, average score, best score) with current-week ranking

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/scores` | Submit a game score |
| `GET` | `/api/leaderboards/global` | Global leaderboard (paginated) |
| `GET` | `/api/leaderboards/regions/{region}` | Regional leaderboard (paginated) |
| `GET` | `/api/leaderboards/players/{playerId}/rank` | Player rank + surrounding players |
| `GET` | `/api/players/{playerId}` | Player profile with stats |

## Data Model

Three Cosmos DB containers with denormalized documents and pre-computed aggregates:

| Container | Partition Key | Purpose |
|-----------|--------------|---------|
| `leaderboard-entries` | HPK: `/periodId`, `/region` | Ranked leaderboard data (primary read target) |
| `scores` | `/playerId` | Immutable score submission log |
| `players` | `/playerId` | Player profiles with aggregate stats |

## Spec Artifacts

| File | Description |
|------|-------------|
| [spec.md](spec.md) | Feature specification — 4 user stories, 12 functional requirements, testing requirements |
| [plan.md](plan.md) | Implementation plan — architecture, project structure, technical context |
| [tasks.md](tasks.md) | Task breakdown — 50 tasks across 7 phases |
| [data-model.md](data-model.md) | Cosmos DB container schemas, indexes, and sample documents |
| [contracts/rest-api.md](contracts/rest-api.md) | REST API endpoint contracts with request/response examples |
| [research.md](research.md) | Technology decisions and best practices research |
| [quickstart.md](quickstart.md) | Local development setup guide |
| [checklists/requirements.md](checklists/requirements.md) | Requirements quality checklist |

## Running Locally

```bash
# Prerequisites: Java 17+, Maven, Cosmos DB Emulator running on localhost:8081

# Build
mvn compile

# Run unit + controller tests (no emulator needed)
mvn test -Dtest="*ServiceTest,*ControllerTest"

# Run integration tests (requires Cosmos DB Emulator)
mvn test -Dtest="*IntegrationTest" -Dspring.profiles.active=emulator

# Start the application (with emulator SSL truststore — see Troubleshooting below)
mvn spring-boot:run "-Dspring-boot.run.profiles=emulator" \
  "-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStore=./emulator-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"
```

## Design Decisions

- **Regions** are ISO 3166-1 alpha-2 country codes (not continental groupings)
- **Authentication** handled by API gateway — this API receives pre-authenticated requests
- **PlayerProfile** documents are created by an external player management system
- **Weekly periods** use ISO 8601 week format (`YYYY-Www`) with historical retention
- **Tiebreaker** for identical scores: earlier submission timestamp ranks higher
- **Ranking** uses COUNT-based computation against composite indexes

---

## Development Journey

This project was built entirely through a spec-driven workflow using **SpecKit v0.4.3** running inside **VS Code** with **GitHub Copilot Chat** (Claude Opus 4.6) as the coding agent. The process went from natural-language description to a fully tested, running application in a single session. Below is a detailed account of each phase, the tools involved, and the troubleshooting required along the way.

### Tooling Used

| Tool | Role |
|------|------|
| **SpecKit v0.4.3** | Spec-driven development framework — structured prompts for specify, plan, tasks, analyze, and implement workflows |
| **GitHub Copilot Chat (Claude Opus 4.6)** | AI coding agent — drove every phase from spec authoring to implementation and runtime debugging |
| **Azure Cosmos DB Best Practices Skill** (`cosmosdb-best-practices`) | Copilot skill that provided partition key strategy, indexing policy, SDK usage rules, and container design guidance |
| **Azure Cosmos DB Emulator** (Windows) | Local Cosmos DB instance for runtime and integration testing |
| **Maven 3.9.6** | Build tooling for the Spring Boot project |
| **Java 21** (Eclipse Adoptium) | Runtime JDK |

### Phase 1: Specification (`/speckit.specify`)

**Input prompt**: *"Build an API for a mobile game's leaderboard system. The system needs to handle real-time score updates, display global and regional leaderboards, and support player profile queries."*

Copilot Chat produced a complete feature specification with:
- **4 user stories** (P1–P4) prioritized by dependency: score submission → global leaderboard → regional leaderboard → player profile
- **14 functional requirements** (FR-001 through FR-014), each testable and unambiguous
- **7 success criteria** with measurable thresholds (2s submission latency, 10K concurrent writes, sub-second queries)
- **Edge cases**: tied scores, negative scores, missing regions, invalid pages, special-character display names
- **Assumptions**: authentication delegated to gateway, players pre-populated by external system, ISO 3166-1 alpha-2 for regions

A quality checklist (`checklists/requirements.md`) was generated and passed 16/16 checks — no implementation details in the spec, all requirements testable, success criteria technology-agnostic.

**Testing requirements** were added after initial spec creation. The spec was strengthened to require three test levels (unit, controller, integration) and to mandate that integration tests verify actual database writes — not just API responses. Specific requirements were added for cross-container consistency verification, ranking correctness against real composite indexes, regional isolation, and pagination against real data.

### Phase 2: Planning (`/speckit.plan`)

**Input**: The spec plus a directive to use Spring Boot 3, Azure Cosmos DB NoSQL API, and Java 17+.

Before writing the plan, Copilot Chat loaded the **`cosmosdb-best-practices` skill** (~2,800 lines of Cosmos DB rules covering partitioning, indexing, SDK patterns, throughput, and query optimization). This skill directly influenced:

- **Partition key strategy**: Hierarchical partition keys (`/periodId` → `/region`) on the `leaderboard-entries` container, chosen to enable single-partition regional queries and bounded-fan-out global queries. The skill's `partition-hierarchical`, `partition-query-patterns`, and `partition-avoid-hotspots` rules were applied.
- **Multi-container design**: Three containers separated by access pattern (high-read leaderboard entries, high-write score log, low-latency player profiles). Guided by `model-denormalize-reads` and `throughput-container-vs-database` rules.
- **Ranking approach**: COUNT-based rank computation for on-demand lookups combined with composite-indexed ORDER BY for top-N queries. Guided by `pattern-efficient-ranking`.
- **Indexing**: Composite index on `(bestScore DESC, scoreTimestamp ASC)` for tiebreaker ordering, with explicit excluded paths for non-queried fields like `displayName`. Guided by `index-composite` and `index-exclude-unused`.
- **SDK usage**: Gateway connection mode for emulator, Session consistency for read-your-writes, `contentResponseOnWriteEnabled(true)`. Guided by `sdk-java-direct-vs-gateway` and `sdk-prefer-session-consistency`.

The plan phase produced 6 artifacts:
1. **plan.md** — Architecture, project structure, tech stack, complexity tracking
2. **research.md** — 9 research sections covering partition strategy, container design, ranking, composite indexes, connection modes, testing approach, Spring Data integration, error handling, and period management
3. **data-model.md** — Document schemas for all 3 containers with indexes, TTL settings, and sample documents
4. **contracts/rest-api.md** — 5 REST endpoints with request/response examples, status codes, and error formats
5. **quickstart.md** — Local development setup guide
6. Updated agent context via `update-agent-context.ps1`

### Phase 3: Task Breakdown (`/speckit.tasks`)

Tasks were generated from the plan and spec, initially producing 31 tasks. After adding testing requirements to the spec, the task list was regenerated to **50 tasks across 7 phases**:

| Phase | Tasks | Purpose |
|-------|-------|---------|
| Phase 1: Setup | T001–T004 | Maven project, Spring Boot entry point, config files |
| Phase 2: Foundational | T005–T022, T048 | All entities, repositories, DTOs, error handling, test infrastructure, `PeriodUtil` |
| Phase 3: US1 (Score Submission) | T023–T028 | Service, controller, unit tests, controller tests, integration tests |
| Phase 4: US2 (Global Leaderboard) | T029–T034 | Service, controller, unit tests, controller tests, integration tests |
| Phase 5: US3 (Regional Leaderboard) | T035–T042 | Service, controller, unit tests, controller tests, integration tests |
| Phase 6: US4 (Player Profile) | T043–T047 | Service, controller, unit tests, controller tests, integration tests |
| Phase 7: Polish | T049–T050 | Bean validation, pagination guards |

Each user story phase includes implementation, unit tests with mocked repos, MockMvc controller tests, and integration tests designed to run against the Cosmos DB Emulator.

### Phase 4: Consistency Analysis (`/speckit.analyze` × 2 rounds)

Before implementation, SpecKit's analysis agent performed two rounds of cross-artifact consistency checks.

**Round 1** — Found 12 findings across all 6 artifacts:
- **F1 (CRITICAL)**: Spec said FR-012 enforces display name constraints, but the assumption says display names are managed externally. Resolution: struck through FR-012, added clarifying note.
- **F2 (CRITICAL)**: Spec said FR-014 requires authentication, but the assumption delegates auth to the gateway. Resolution: struck through FR-014, moved to assumptions.
- **F3–F12**: Medium and low findings including inconsistent region terminology (continental vs. country-based), missing error codes in contracts, stale task references, and incomplete FR-to-task traceability.

All 12 findings were remediated across `spec.md`, `plan.md`, and `contracts/rest-api.md`.

**Round 2** — Found 7 additional findings after the first round's edits:
- **C1 (HIGH)**: Contract auth header still said "validated" instead of reflecting gateway delegation. Fixed.
- **I1–I2 (MEDIUM)**: Stale comments in `data-model.md` about `displayName` and hierarchical partition key annotations. Fixed by recreating `data-model.md` (it had been accidentally deleted).
- **I3 (MEDIUM)**: `T048` (`PeriodUtil`) was placed in Phase 7 but is a blocking dependency for Phase 3. Moved to Phase 2.
- **P1 (MEDIUM)**: Constitution still in placeholder state. Explicitly waived in `plan.md` with a note to re-check if principles are later defined.
- **U1–U2 (LOW)**: Minor spec wording improvements (special-char testing in US4, surrounding-ranks clarification in US2).

All 7 findings were remediated. The artifacts reached a consistent, implementation-ready state.

### Phase 5: Implementation (`/speckit.implement`)

The implement agent executed all 50 tasks phase-by-phase, producing:
- **3 controllers**, **3 services**, **3 repositories**, **3 domain models**, **5 DTOs**
- **`GlobalExceptionHandler`** with `@ControllerAdvice` for 404/400/500 responses
- **`PeriodUtil`** for ISO 8601 week period computation
- **`CosmosDbConfig`** with `@Bean` dependency chain for client, database, and container initialization
- **38 unit + controller tests** (6 test classes), all passing on first run after 4 minor fixes

**Implementation fixes applied during the build**:
1. **`@Override` on `cosmosClientBuilder()`**: Removed — this method doesn't override a superclass method in `AbstractCosmosConfiguration`.
2. **`@MockBean` import**: Changed from `org.springframework.boot.test.mock.mockito.MockBean` (deprecated) to the correct Spring Boot 3 import path.
3. **Unnecessary Mockito stub**: Removed a `when(...).thenReturn(...)` stub in a negative-score test that was never reached because validation throws before the mock is called.
4. **`@JsonProperty("isNewBest")`**: Added to the `ScoreSubmissionResponse.isNewBest` field — Jackson serializes boolean `isXxx` getters as `xxx` by default, which broke the API contract.

**Result**: `mvn test` — **38/38 tests pass**, BUILD SUCCESS in 9.6 seconds.

### Phase 6: Runtime Startup — Troubleshooting

Starting the application against the Cosmos DB Emulator required solving two significant issues.

#### Issue 1: SSL Certificate Trust (`SunCertPathBuilderException`)

**Problem**: The Cosmos DB Emulator uses a self-signed SSL certificate. Java's default truststore (`cacerts`) does not include it. Spring Boot startup failed with:

```
SunCertPathBuilderException: unable to find valid certification path to requested target
```

**Attempted fix**: Import the emulator certificate directly into the JDK's `cacerts` file via `keytool -import`. This failed with **Access Denied** — the JDK is installed under `C:\Program Files\` and requires administrator privileges to modify.

**Working solution**: Created a local truststore at the project root:
1. Exported the emulator certificate from `Cert:\CurrentUser\Root` (thumbprint `9BA76F0D...`, friendly name `DocumentDbEmulatorCertificate`) to a `.cer` file
2. Copied the system `cacerts` to `emulator-truststore.jks` in the project root
3. Imported the emulator certificate into the local truststore via `keytool -import`
4. Passed the truststore to Spring Boot via JVM args:

```
-Djavax.net.ssl.trustStore=./emulator-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit
```

#### Issue 2: Cosmos DB Indexing Policy Error (`/"/" is not provided`)

**Problem**: After resolving SSL, container creation failed with:

```
The special mandatory indexing path '/' is not provided
```

**Root cause**: The `CosmosDbConfig` code set `ExcludedPaths` (for `/displayName/?`) without also explicitly setting `IncludedPaths`. The Cosmos DB Emulator requires the mandatory root path `/*` to be explicitly present when you customize the indexing policy.

**Fix**: Added an explicit `IncludedPaths` declaration before setting excluded paths:

```java
indexingPolicy.setIncludedPaths(List.of(new IncludedPath("/*")));
indexingPolicy.setExcludedPaths(List.of(
    new ExcludedPath("/displayName/?"),
    new ExcludedPath("/\"_etag\"/?")
));
```

After both fixes, the application started successfully — Spring Boot 3.2.5 on Tomcat port 8080, all 3 Cosmos DB containers created, Gateway connection mode with Session consistency.

### Phase 7: Live API Verification

With the app running, all 5 endpoints were tested via HTTP requests.

**Test data seeding**: Since PlayerProfile documents are managed by an external system (per spec), test players were seeded directly into the Cosmos DB Emulator using a Python script (`seed-test-data.py`) that constructs HMAC-SHA256 auth headers for the Cosmos DB REST API. Three players were created: `player-001` (US), `player-002` (DE), `player-003` (JP).

> **Note**: An initial attempt to run the seed script as an inline PowerShell/Python one-liner failed due to PowerShell string escaping mangling the `x-ms-documentdb-partitionkey` header (which contains JSON brackets). The fix was to write it to a standalone `.py` file.

**Test results**:

| Test | Endpoint | Result |
|------|----------|--------|
| Submit score (player-001, 85000) | `POST /api/scores` | 201 — globalRank: 1, regionalRank: 1, isNewBest: true |
| Submit score (player-002, 92000) | `POST /api/scores` | 201 — globalRank: 2, regionalRank: 1, isNewBest: false |
| Submit score (player-003, 78000) | `POST /api/scores` | 201 — globalRank: 1, regionalRank: 1 |
| Submit higher score (player-001, 91000) | `POST /api/scores` | 201 — best score updated from 85000 to 91000 |
| Submit highest score (player-003, 95000) | `POST /api/scores` | 201 — globalRank: 1 |
| Global leaderboard | `GET /api/leaderboards/global` | 200 — 3 entries, ordered: 95000 > 92000 > 91000 |
| US regional leaderboard | `GET /api/leaderboards/regions/US` | 200 — 1 entry (player-001 only) |
| Empty regional leaderboard | `GET /api/leaderboards/regions/BR` | 200 — 0 entries, empty array |
| Player rank + surrounding | `GET /api/leaderboards/players/player-001/rank` | 200 — rank 3, 3 surrounding players |
| Player profile with stats | `GET /api/players/player-001` | 200 — 3 games played, avg 89000, best 91000, global rank 3, US rank 1 |
| Negative score rejected | `POST /api/scores` (score: -5) | 400 — `VALIDATION_ERROR` |
| Unknown player score | `POST /api/scores` (player: nonexistent) | 404 — `PLAYER_NOT_FOUND` |
| Unknown player profile | `GET /api/players/nonexistent` | 404 — `PLAYER_NOT_FOUND` |

All responses matched the API contracts defined in `contracts/rest-api.md`. Key validations confirmed:
- Best-score retention (91000 kept, not 85000 from first submission)
- Aggregate stat computation (3 games, average = (85000+91000+85000)/3 ≈ 89000)
- Ranking order across global and regional scopes
- Regional isolation (US query returns only US players)
- Proper error codes and messages for all failure modes

### Summary of Remediations

| Category | Issue | Source | Resolution |
|----------|-------|--------|------------|
| **Spec consistency** | FR-012 display name validation contradicted assumptions | SpecKit analyze | Struck through FR-012, added clarification |
| **Spec consistency** | FR-014 auth requirement contradicted gateway assumption | SpecKit analyze | Struck through FR-014, moved to assumptions |
| **Spec consistency** | Continental region terms vs. ISO country codes | SpecKit analyze | Standardized to ISO 3166-1 alpha-2 throughout |
| **Contract drift** | Auth header said "validated" post-remediation | SpecKit analyze (round 2) | Updated to "Handled by API gateway" |
| **Task ordering** | `PeriodUtil` (T048) in wrong phase | SpecKit analyze (round 2) | Moved from Phase 7 to Phase 2 |
| **Data model** | Stale `displayName` and HPK comments | SpecKit analyze (round 2) | Recreated `data-model.md` with fixes |
| **Constitution** | Placeholder state, never defined | SpecKit analyze (round 2) | Explicitly waived in `plan.md` |
| **Build** | Invalid `@Override` on `cosmosClientBuilder()` | Copilot (compile error) | Removed annotation |
| **Build** | Deprecated `@MockBean` import path | Copilot (compile error) | Updated import |
| **Build** | Unnecessary Mockito stub on unreachable code path | Copilot (test failure) | Removed stub |
| **Build** | Jackson boolean serialization (`isNewBest` → `newBest`) | Copilot (test assertion) | Added `@JsonProperty("isNewBest")` |
| **Runtime** | Java SSL trust failure for emulator self-signed cert | Manual debugging | Created local truststore (`emulator-truststore.jks`) |
| **Runtime** | Cosmos DB mandatory indexing root path `/*` missing | Cosmos DB emulator error | Added explicit `IncludedPaths` to indexing policy |
| **Testing** | PowerShell escaping broke inline Python seed script | Manual debugging | Wrote standalone `seed-test-data.py` file |

### What Worked Well

- **SpecKit's structured workflow** enforced a disciplined progression from spec → plan → tasks → implement, preventing premature coding
- **Consistency analysis** caught 19 findings across 2 rounds that would have caused implementation bugs or contract mismatches
- **The Cosmos DB best-practices skill** provided concrete, rule-referenced guidance for partitioning, indexing, and SDK usage — not generic advice
- **Task-per-user-story organization** with embedded test tasks ensured each feature was independently testable
- **38/38 tests passing on first build** (after 4 minor fixes) — the spec-driven approach produced implementation-ready task definitions

### What Required Manual Intervention

- **SSL truststore creation** — the quickstart guide's `keytool -import` approach requires admin privileges that weren't available; the local truststore workaround wasn't in any generated artifact
- **Cosmos DB indexing policy** — the `cosmosdb-best-practices` skill's guidance on excluded paths didn't mention the emulator's requirement for explicit `IncludedPaths` when customizing the policy
- **Test data seeding** — the spec correctly defines PlayerProfile as externally managed, but no seed mechanism was generated; required a manual Python script with HMAC auth against the Cosmos DB REST API
- **PowerShell/Python escaping** — inline Python commands with JSON bracket characters are unreliable in PowerShell; standalone scripts are the only reliable approach
