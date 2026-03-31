# Tasks: Game Leaderboard API

**Input**: Design documents from `/specs/001-game-leaderboard-api/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/rest-api.md, quickstart.md

**Tests**: Requested in spec.md Testing Requirements section. Unit tests (mocked repos), MockMvc controller tests, and integration tests against Cosmos DB Emulator are included in each user story phase.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/com/game/leaderboard/` and `src/main/resources/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize Spring Boot project, Maven dependencies, and configuration files

- [x] T001 Create Maven project with `pom.xml` containing Spring Boot 3.2.x parent, Java 17, spring-boot-starter-web, azure-spring-data-cosmos, azure-cosmos SDK 4.52+ dependencies in `pom.xml`
- [x] T002 Create Spring Boot entry point in `src/main/java/com/game/leaderboard/LeaderboardApplication.java`
- [x] T003 [P] Create `src/main/resources/application.yml` with Cosmos DB connection config (endpoint, key, database name, query metrics)
- [x] T004 [P] Create `src/main/resources/application-emulator.yml` with emulator-specific overrides (Gateway mode, localhost endpoint, well-known emulator key)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Cosmos DB configuration, all entity models, all repositories, error handling, DTOs, and test infrastructure that every user story depends on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 Create `CosmosDbConfig` configuration class with @Bean dependency chain (not @PostConstruct) for CosmosClient, database, and 3 containers (`leaderboard-entries`, `scores`, `players`) with composite indexes and hierarchical partition keys in `src/main/java/com/game/leaderboard/config/CosmosDbConfig.java`
- [x] T006 [P] Create `LeaderboardEntry` entity with `@Container`, `@Id`, `@GeneratedValue`, `@PartitionKey` (periodId), String fields for region/playerId/displayName, long bestScore, String scoreTimestamp, type discriminator, schemaVersion in `src/main/java/com/game/leaderboard/model/LeaderboardEntry.java`
- [x] T007 [P] Create `ScoreEntry` entity with `@Container`, `@Id`, `@GeneratedValue`, `@PartitionKey` (playerId), long score, String submittedAt/periodId/region, type discriminator, schemaVersion in `src/main/java/com/game/leaderboard/model/ScoreEntry.java`
- [x] T008 [P] Create `PlayerProfile` entity with `@Container`, `@Id`, `@GeneratedValue`, `@PartitionKey` (playerId), String displayName/region, long bestScore, int totalGamesPlayed, double averageScore, long totalScoreSum, String registeredAt/lastUpdated, type discriminator, schemaVersion in `src/main/java/com/game/leaderboard/model/PlayerProfile.java`
- [x] T009 [P] Create `LeaderboardEntryRepository` extending `CosmosRepository<LeaderboardEntry, String>` with custom `@Query` for top-N by score DESC (with periodId filter), COUNT query for rank, and surrounding-players query in `src/main/java/com/game/leaderboard/repository/LeaderboardEntryRepository.java`
- [x] T010 [P] Create `ScoreEntryRepository` extending `CosmosRepository<ScoreEntry, String>` in `src/main/java/com/game/leaderboard/repository/ScoreEntryRepository.java`
- [x] T011 [P] Create `PlayerProfileRepository` extending `CosmosRepository<PlayerProfile, String>` in `src/main/java/com/game/leaderboard/repository/PlayerProfileRepository.java`
- [x] T012 [P] Create `ScoreSubmissionRequest` DTO with playerId (String, required) and score (long, 0–999,999,999 validated) in `src/main/java/com/game/leaderboard/dto/ScoreSubmissionRequest.java`
- [x] T013 [P] Create `ScoreSubmissionResponse` DTO with playerId, score, isNewBest, periodId, globalRank, regionalRank, region in `src/main/java/com/game/leaderboard/dto/ScoreSubmissionResponse.java`
- [x] T014 [P] Create `LeaderboardResponse` DTO with periodId, scope, page, pageSize, totalEntries, List<LeaderboardEntryDto> entries (each with rank, playerId, displayName, bestScore, region, scoreTimestamp) in `src/main/java/com/game/leaderboard/dto/LeaderboardResponse.java`
- [x] T015 [P] Create `PlayerRankResponse` DTO with playerId, displayName, bestScore, rank, scope, periodId, List<SurroundingPlayerDto> surroundingPlayers in `src/main/java/com/game/leaderboard/dto/PlayerRankResponse.java`
- [x] T016 [P] Create `PlayerProfileResponse` DTO with playerId, displayName, region, bestScore, totalGamesPlayed, averageScore, registeredAt, currentWeekRank (global + regional + periodId) in `src/main/java/com/game/leaderboard/dto/PlayerProfileResponse.java`
- [x] T017 [P] Create `PlayerNotFoundException` extending RuntimeException in `src/main/java/com/game/leaderboard/exception/PlayerNotFoundException.java`
- [x] T018 Create `GlobalExceptionHandler` with `@ControllerAdvice` handling PlayerNotFoundException (404), IllegalArgumentException (400), and generic exceptions (500) returning error/message/timestamp JSON in `src/main/java/com/game/leaderboard/exception/GlobalExceptionHandler.java`
- [x] T019 [P] Add `spring-boot-starter-test` (JUnit 5, MockMvc, Mockito) to test dependencies in `pom.xml`
- [x] T020 [P] Create `src/test/resources/application-emulator.yml` with Cosmos DB Emulator connection config (Gateway mode, localhost:8081, well-known emulator key) for integration tests
- [x] T021 Create base integration test class `AbstractCosmosIntegrationTest` with `@SpringBootTest`, `@ActiveProfiles("emulator")`, and `@BeforeEach` cleanup that deletes all documents from all 3 containers to ensure test isolation in `src/test/java/com/game/leaderboard/integration/AbstractCosmosIntegrationTest.java`
- [x] T022 [P] Create test data builder/factory `TestDataFactory` with methods to create `PlayerProfile`, `ScoreEntry`, and `LeaderboardEntry` instances with sensible defaults and builder-style overrides for tests in `src/test/java/com/game/leaderboard/TestDataFactory.java`
- [x] T048 [P] Create ISO week period utility method to compute `periodId` (format `YYYY-Www`) from a given timestamp, and a method to get the current period, in `src/main/java/com/game/leaderboard/service/PeriodUtil.java`

**Checkpoint**: Foundation ready — all entities, repositories, DTOs, config, error handling, period utility, and test infrastructure in place. User story implementation can now begin.

---

## Phase 3: User Story 1 — Submit and Update Scores in Real Time (Priority: P1) 🎯 MVP

**Goal**: Players submit scores; system persists to `scores` container, conditionally updates `leaderboard-entries` (if new best), updates `players` aggregate stats, returns updated rank.

**Independent Test**: Submit scores for multiple players via `POST /api/scores`. Verify highest score retained, rank returned, player stats updated. Lower scores do not overwrite higher ones.

### Implementation for User Story 1

- [x] T023 [US1] Implement `ScoreService` with `submitScore(ScoreSubmissionRequest)` method: validate score range (0–999,999,999), look up player from `PlayerProfileRepository` (throw PlayerNotFoundException if missing, reject if no region), compute `periodId` from current ISO week, write `ScoreEntry` to `scores` container, read/upsert `LeaderboardEntry` if new best score, update `PlayerProfile` aggregates (totalGamesPlayed++, totalScoreSum+=score, averageScore recalc, bestScore=max), compute global and regional rank via COUNT queries, return `ScoreSubmissionResponse` in `src/main/java/com/game/leaderboard/service/ScoreService.java`
- [x] T024 [US1] Implement `ScoreController` with `@PostMapping("/api/scores")` accepting `@Valid @RequestBody ScoreSubmissionRequest`, delegating to `ScoreService`, returning 201 Created with `ScoreSubmissionResponse` in `src/main/java/com/game/leaderboard/controller/ScoreController.java`

### Unit Tests for User Story 1

- [x] T025 [P] [US1] Create `ScoreServiceTest` with mocked repositories: test submitScore happy path (new score persisted, leaderboard entry created, player stats updated, response contains rank), test higher-score-retained (submit 800 then 300 — bestScore stays 800), test lower-score-no-overwrite (verify leaderboard entry not upserted), test score boundary values (0 accepted, 999999999 accepted), test negative score rejected, test missing player throws PlayerNotFoundException, test player without region throws IllegalArgumentException, test averageScore recalculation correctness in `src/test/java/com/game/leaderboard/service/ScoreServiceTest.java`
- [x] T026 [P] [US1] Create `ScoreControllerTest` with MockMvc: test POST /api/scores returns 201 with valid request, test 400 for missing playerId, test 400 for score out of range, test 400 for player without region, test 404 for non-existent player, test response body structure (playerId, score, isNewBest, globalRank, regionalRank, periodId) in `src/test/java/com/game/leaderboard/controller/ScoreControllerTest.java`

### Integration Tests for User Story 1

- [x] T027 [US1] Create `ScoreSubmissionIntegrationTest` extending `AbstractCosmosIntegrationTest`: insert a PlayerProfile (simulating external player management system) into `players` container via repository, submit score via POST /api/scores, then read back from all 3 containers — assert ScoreEntry exists in `scores` with correct playerId/score/periodId, assert LeaderboardEntry exists in `leaderboard-entries` with correct bestScore, assert PlayerProfile in `players` has updated totalGamesPlayed/totalScoreSum/averageScore/bestScore. Test cross-container consistency by verifying all 3 containers after a single submission in `src/test/java/com/game/leaderboard/integration/ScoreSubmissionIntegrationTest.java`
- [x] T028 [US1] Add test cases to `ScoreSubmissionIntegrationTest`: submit score 800 then 300 for same player — read back LeaderboardEntry and assert bestScore is still 800, read back PlayerProfile and assert totalGamesPlayed=2 and averageScore=(800+300)/2. Submit score for player without region — assert 400 response and no documents written to any container in `src/test/java/com/game/leaderboard/integration/ScoreSubmissionIntegrationTest.java`

**Checkpoint**: Score submission fully functional and verified against real database. MVP deliverable.

---

## Phase 4: User Story 2 — View Global Leaderboard (Priority: P2)

**Goal**: Players query the global top-N leaderboard for the current (or specified) week with pagination.

**Independent Test**: Populate scores for multiple players across regions, then `GET /api/leaderboards/global`. Verify results sorted by bestScore DESC with scoreTimestamp ASC tiebreaker, correct pagination (page/pageSize), correct totalEntries count.

### Implementation for User Story 2

- [x] T029 [US2] Implement `LeaderboardService.getGlobalLeaderboard(periodId, page, pageSize)` method: default periodId to current ISO week if null, validate page ≥ 1 and pageSize 1–100, query `LeaderboardEntryRepository` with ORDER BY bestScore DESC + scoreTimestamp ASC across all regions for the periodId (bounded cross-partition prefix query), apply offset/limit pagination, compute rank numbers, count total entries, return `LeaderboardResponse` in `src/main/java/com/game/leaderboard/service/LeaderboardService.java`
- [x] T030 [US2] Implement `LeaderboardController` with `@GetMapping("/api/leaderboards/global")` accepting `@RequestParam` for periodId, page, pageSize, delegating to `LeaderboardService`, returning 200 OK with `LeaderboardResponse` in `src/main/java/com/game/leaderboard/controller/LeaderboardController.java`

### Unit Tests for User Story 2

- [x] T031 [P] [US2] Create `LeaderboardServiceTest` with mocked repository: test getGlobalLeaderboard happy path (returns entries sorted by score DESC), test default periodId to current week when null, test page/pageSize validation (page 0 rejected, pageSize 101 rejected), test empty leaderboard returns empty entries with totalEntries=0, test rank numbering is correct (rank 1 = highest score) in `src/test/java/com/game/leaderboard/service/LeaderboardServiceTest.java`
- [x] T032 [P] [US2] Create `LeaderboardControllerTest` with MockMvc: test GET /api/leaderboards/global returns 200 with valid params, test default pagination (page=1, pageSize=50), test 400 for invalid page/pageSize, test response structure (periodId, scope, page, pageSize, totalEntries, entries array) in `src/test/java/com/game/leaderboard/controller/LeaderboardControllerTest.java`

### Integration Tests for User Story 2

- [x] T033 [US2] Create `GlobalLeaderboardIntegrationTest` extending `AbstractCosmosIntegrationTest`: insert 10 LeaderboardEntry documents with known scores into `leaderboard-entries` container via repository, GET /api/leaderboards/global, assert entries returned in descending score order matching inserted data exactly. Test tied scores (insert 2 entries with score=500 but different timestamps) — assert earlier timestamp ranks higher, verifying actual composite index behavior in `src/test/java/com/game/leaderboard/integration/GlobalLeaderboardIntegrationTest.java`
- [x] T034 [US2] Add pagination tests to `GlobalLeaderboardIntegrationTest`: insert 75 LeaderboardEntry documents, request page=1 pageSize=50 — assert 50 entries returned with totalEntries=75, request page=2 pageSize=50 — assert 25 entries returned, verify no duplicates between pages and no gaps (union of both pages = all 75 entries) in `src/test/java/com/game/leaderboard/integration/GlobalLeaderboardIntegrationTest.java`

**Checkpoint**: Global leaderboard functional and verified with real Cosmos DB queries, ordering, and pagination.

> **Note**: US2 acceptance scenario 3 ("find my rank") is fulfilled by US3's T036/T038 (getPlayerRank). FR-007 coverage is in Phase 5.

---

## Phase 5: User Story 3 — View Regional Leaderboard (Priority: P3)

**Goal**: Players query the regional top-N leaderboard filtered by ISO 3166-1 alpha-2 country code (e.g., US, DE, JP) for the current (or specified) week with pagination. Also implements "find my rank" (FR-007) for both global and regional scopes.

**Independent Test**: Populate scores for players in multiple countries, then `GET /api/leaderboards/regions/US`. Verify only US players returned, sorted correctly. Query `GET /api/leaderboards/regions/XX` for invalid country code returns 400.

### Implementation for User Story 3

- [x] T035 [US3] Implement `LeaderboardService.getRegionalLeaderboard(region, periodId, page, pageSize)` method: validate region is valid ISO 3166-1 alpha-2 code (throw IllegalArgumentException if not), default periodId to current ISO week, query `LeaderboardEntryRepository` with periodId + region (single-partition query) ORDER BY bestScore DESC + scoreTimestamp ASC, apply pagination, compute rank numbers, count total entries, return `LeaderboardResponse` with scope "regional" in `src/main/java/com/game/leaderboard/service/LeaderboardService.java`
- [x] T036 [US3] Implement `LeaderboardService.getPlayerRank(playerId, periodId, scope, surrounding)` method: look up player's `LeaderboardEntry` for the period, compute rank via COUNT query (count entries with bestScore > player's score), fetch surrounding ±N players with TOP query centered on player's score, return `PlayerRankResponse` in `src/main/java/com/game/leaderboard/service/LeaderboardService.java`
- [x] T037 [US3] Add `@GetMapping("/api/leaderboards/regions/{region}")` to `LeaderboardController` accepting region path variable and periodId/page/pageSize query params, delegating to `LeaderboardService.getRegionalLeaderboard()` in `src/main/java/com/game/leaderboard/controller/LeaderboardController.java`
- [x] T038 [US3] Add `@GetMapping("/api/leaderboards/players/{playerId}/rank")` to `LeaderboardController` accepting playerId path variable and periodId/scope/surrounding query params, delegating to `LeaderboardService.getPlayerRank()` in `src/main/java/com/game/leaderboard/controller/LeaderboardController.java`

### Unit Tests for User Story 3

- [x] T039 [P] [US3] Add regional tests to `LeaderboardServiceTest`: test getRegionalLeaderboard returns only entries matching region, test invalid region code throws IllegalArgumentException, test getPlayerRank computes correct rank from COUNT, test surrounding players returned in correct order, test player not found for rank query in `src/test/java/com/game/leaderboard/service/LeaderboardServiceTest.java`
- [x] T040 [P] [US3] Add regional tests to `LeaderboardControllerTest`: test GET /api/leaderboards/regions/{region} returns 200, test 400 for invalid region code, test GET /api/leaderboards/players/{playerId}/rank returns 200 with rank and surrounding players, test 404 for non-existent player rank in `src/test/java/com/game/leaderboard/controller/LeaderboardControllerTest.java`

### Integration Tests for User Story 3

- [x] T041 [US3] Create `RegionalLeaderboardIntegrationTest` extending `AbstractCosmosIntegrationTest`: insert LeaderboardEntry documents for players in US, DE, and JP countries, GET /api/leaderboards/regions/US — assert only US players returned and zero entries from DE or JP appear. Insert entries in a country with no scores, GET that country — assert empty result set. Verify regional isolation by asserting exact count matches inserted country data in `src/test/java/com/game/leaderboard/integration/RegionalLeaderboardIntegrationTest.java`
- [x] T042 [US3] Create `PlayerRankIntegrationTest` extending `AbstractCosmosIntegrationTest`: insert 20 LeaderboardEntry documents with known scores, GET /api/leaderboards/players/{playerId}/rank for a player ranked ~10th — assert correct rank number and surrounding players match database state. Test with tied scores to verify tiebreaker ordering matches actual Cosmos DB results in `src/test/java/com/game/leaderboard/integration/PlayerRankIntegrationTest.java`

**Checkpoint**: Regional leaderboards, player rank lookup, and regional isolation verified against real database.

---

## Phase 6: User Story 4 — Query Player Profile (Priority: P4)

**Goal**: Players query a specific player's profile with cumulative stats (totalGamesPlayed, bestScore, averageScore) and current-week ranking.

**Independent Test**: Create a player, submit scores, then `GET /api/players/{playerId}`. Verify profile fields match (displayName, region, bestScore, totalGamesPlayed, averageScore, registeredAt) and currentWeekRank includes global and regional ranks for current period.

### Implementation for User Story 4

- [x] T043 [US4] Implement `PlayerService.getPlayerProfile(playerId)` method: point-read `PlayerProfile` from `players` container (1 RU), throw `PlayerNotFoundException` if missing, compute current-week global and regional rank from `LeaderboardService`, map to `PlayerProfileResponse` in `src/main/java/com/game/leaderboard/service/PlayerService.java`
- [x] T044 [US4] Implement `PlayerController` with `@GetMapping("/api/players/{playerId}")` accepting playerId path variable, delegating to `PlayerService`, returning 200 OK with `PlayerProfileResponse` or 404 in `src/main/java/com/game/leaderboard/controller/PlayerController.java`

### Unit Tests for User Story 4

- [x] T045 [P] [US4] Create `PlayerServiceTest` with mocked repositories: test getPlayerProfile happy path (returns profile with ranks), test PlayerNotFoundException for non-existent playerId, test profile fields mapped correctly (displayName, region, bestScore, totalGamesPlayed, averageScore) in `src/test/java/com/game/leaderboard/service/PlayerServiceTest.java`
- [x] T046 [P] [US4] Create `PlayerControllerTest` with MockMvc: test GET /api/players/{playerId} returns 200 with valid player, test 404 for non-existent player, test response body structure matches PlayerProfileResponse in `src/test/java/com/game/leaderboard/controller/PlayerControllerTest.java`

### Integration Tests for User Story 4

- [x] T047 [US4] Create `PlayerProfileIntegrationTest` extending `AbstractCosmosIntegrationTest`: insert a PlayerProfile (simulating external player management system) and matching LeaderboardEntry documents into their respective containers via repositories, GET /api/players/{playerId} — assert all profile fields match persisted PlayerProfile document exactly (read back from `players` container to compare), assert currentWeekRank global and regional values match rank computed from actual leaderboard-entries data. Test after score submission: insert player, submit score via POST /api/scores, then GET profile — assert stats reflect the submission (read back from `players` container to verify totalGamesPlayed, averageScore, bestScore). Test display name round-trip: insert a PlayerProfile with Unicode/special character display name (e.g., emoji, CJK characters, max-length string), submit score, GET profile — assert displayName survives write-read cycle intact in `src/test/java/com/game/leaderboard/integration/PlayerProfileIntegrationTest.java`

**Checkpoint**: Player profile queries functional and verified. All 4 user stories independently tested against real database.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validation, input sanitization, and configuration polish

- [x] T049 [P] Add Jakarta Bean Validation annotations (`@NotBlank`, `@Min`, `@Max`) to `ScoreSubmissionRequest` and enable `@Valid` on controller method parameter in `src/main/java/com/game/leaderboard/dto/ScoreSubmissionRequest.java`
- [x] T050 Add validation for page/pageSize query params across all leaderboard controller endpoints (page ≥ 1, pageSize 1–100, default 50) in `src/main/java/com/game/leaderboard/controller/LeaderboardController.java`

---

## Dependencies

```
Phase 1 (Setup)
  └─► Phase 2 (Foundational + Test Infrastructure)
        ├─► Phase 3 (US1: Score Submission + Tests) 🎯 MVP
        │     └─► Phase 4 (US2: Global Leaderboard + Tests)
        │           └─► Phase 5 (US3: Regional Leaderboard + Tests)
        │                 └─► Phase 6 (US4: Player Profile + Tests)
        └─► Phase 7 (Polish) — can start after Phase 2, parallel with user stories
```

**Notes**:
- US2 depends on US1 (needs score data to display leaderboards)
- US3 depends on US2 (extends the LeaderboardService and LeaderboardController created in US2)
- US4 depends on US3 (uses `getPlayerRank()` from LeaderboardService for current-week ranks)
- Phase 7 tasks (T049–T050) are parallelizable with user story phases
- Within each user story phase: implementation tasks first, then unit/controller tests [P], then integration tests (integration tests depend on the implementation being complete)

## Parallel Execution Opportunities

| Tasks | Why Parallel |
|-------|-------------|
| T003, T004 | Independent config files |
| T006, T007, T008 | Independent entity classes in separate files |
| T009, T010, T011 | Independent repository interfaces in separate files |
| T012–T016 | Independent DTO classes in separate files |
| T017, T019, T020, T022 | Independent foundational files (exception, pom test deps, test config, test factory) |
| T025, T026 | US1 unit tests + controller tests (different test files, both depend on T023-T024) |
| T031, T032 | US2 unit tests + controller tests (different test files, both depend on T029-T030) |
| T039, T040 | US3 unit tests + controller tests (different test files, both depend on T035-T038) |
| T045, T046 | US4 unit tests + controller tests (different test files, both depend on T043-T044) |
| T049, T050 | Independent polish tasks across different files |

## Implementation Strategy

1. **MVP**: Complete Phase 1 → Phase 2 → Phase 3 (including tests). At this point, `POST /api/scores` is fully functional with unit, controller, and integration tests verifying writes land in all 3 containers.
2. **Incremental delivery**: Phase 4 adds global leaderboard with query verification tests, Phase 5 adds regional filtering with isolation tests, Phase 6 adds player profiles with read-back verification.
3. **Polish tasks** (Phase 7) can be interleaved after Phase 2. T048 (PeriodUtil) is now in Phase 2, available from the start of Phase 3.
4. **Test-alongside pattern**: Each user story phase includes implementation → unit/controller tests (parallel) → integration tests. Tests run against Cosmos DB Emulator to verify actual database writes and queries.
