# I Built a Production-Ready API in One Session Using Spec-Driven Development with AI

I gave an AI agent a single sentence — *"Build an API for a mobile game's leaderboard system"* — and walked away with a fully implemented Spring Boot API backed by Azure Cosmos DB, 38 passing tests, and a running application verified against real API calls. No boilerplate templates. No copy-pasting from Stack Overflow. No half-finished TODO comments left in the code.

But here's the thing: the AI didn't just start writing code. It wrote a spec first. Then a plan. Then it analyzed its own work for contradictions, found 19 of them across two rounds, fixed every one, and *then* started coding. The tests were designed during planning, before a single line of Java existed.

This is what spec-driven development looks like when you pair it with the right tooling.

## The Stack

- **SpecKit v0.4.3** — A spec-driven development framework that runs inside VS Code. It provides structured prompts for each phase: `/speckit.specify`, `/speckit.plan`, `/speckit.tasks`, `/speckit.analyze`, and `/speckit.implement`. Each prompt enforces a specific output format and quality gate before the next phase can begin.
- **GitHub Copilot (Claude)** — The AI coding agent driving every phase. Not just autocomplete — this is an agentic workflow where Copilot reads files, makes decisions, writes code, runs builds, and debugs failures.
- **Azure Cosmos DB Best Practices Skill** — A Copilot skill containing ~2,800 lines of Cosmos DB-specific rules covering partitioning, indexing, SDK patterns, throughput, and query optimization. Each rule has an ID (like `partition-hierarchical` or `index-composite`) so you can trace exactly which rule influenced which decision.
- **Azure Cosmos DB Emulator** — The Windows emulator running locally for both development and integration testing.

## What I Built

A REST API for a mobile game leaderboard system. Five endpoints:

| Method | Path | What It Does |
|--------|------|-------------|
| `POST` | `/api/scores` | Submit a game score |
| `GET` | `/api/leaderboards/global` | Global leaderboard, paginated |
| `GET` | `/api/leaderboards/regions/{region}` | Regional leaderboard by country |
| `GET` | `/api/leaderboards/players/{playerId}/rank` | A player's rank with surrounding context |
| `GET` | `/api/players/{playerId}` | Player profile with cumulative stats |

Backed by three Cosmos DB containers (leaderboard entries, score log, player profiles), with hierarchical partition keys, composite indexes for tiebreaker ordering, and denormalized documents for read performance. Weekly leaderboard periods using ISO 8601 week format. COUNT-based ranking. Session consistency for read-your-writes guarantees.

The kind of design you'd expect from a team that spent a few days whiteboarding. I spent zero minutes whiteboarding.

## Phase 1: Specification — One Sentence to Four User Stories

I ran `/speckit.specify` and typed:

> Build an API for a mobile game's leaderboard system. The system needs to handle real-time score updates, display global and regional leaderboards, and support player profile queries.

What came back was a complete feature specification:

- **4 user stories** prioritized by dependency (score submission first, because nothing else works without scores)
- **14 functional requirements**, each testable and specific — not vague "the system should" handwaving
- **7 success criteria** with measurable thresholds: score submissions acknowledged within 2 seconds, 10,000 concurrent writes without data loss, leaderboard queries under 1 second for up to 1 million entries
- **Edge cases**: What happens with tied scores? Negative scores? A region with no players? A display name with emoji?
- **Assumptions**: Authentication is the gateway's problem. Players are created by another system. Regions use ISO 3166-1 alpha-2 country codes.

SpecKit also generated a quality checklist — 16 checkpoints validating that the spec contained no implementation details, all requirements were testable, and success criteria were technology-agnostic. It passed 16/16.

But I wasn't done with the spec. I went back and added a **Testing Requirements** section. This is where things got interesting.

### Tests as Requirements, Not Afterthoughts

Most projects treat tests as a post-implementation activity. You write the code, then you write tests to cover it. The problem with that approach is clear — your tests are shaped by what you built, not by what you intended to build.

I added testing requirements directly to the spec at three levels:

1. **Unit tests** — Every service class gets tests with mocked repositories. Happy paths, edge cases, boundary values. Tiebreaker logic explicitly tested.
2. **Controller tests** — Every endpoint gets MockMvc tests. HTTP status codes, request validation, response structure, error responses.
3. **Integration tests** — And this is the part I pushed hard on. The spec now required that integration tests:
   - **Verify actual database writes** — not just check the API response. After submitting a score, read back from all three Cosmos DB containers and assert the documents match.
   - **Test cross-container consistency** — A single score submission touches `scores`, `leaderboard-entries`, and `players`. All three must be correct.
   - **Validate ranking against real composite indexes** — Insert known scores, query the leaderboard, and assert the ranking matches expected positions. Include tied scores to verify the timestamp tiebreaker works with the real Cosmos DB composite index, not just mocked behavior.
   - **Confirm regional isolation** — Insert players in multiple countries, query one country, and assert zero cross-contamination.
   - **Test pagination against real data** — Insert enough entries to span multiple pages, query each page, and verify no duplicates or gaps.

These requirements went into the spec before any code existed. When the tasks were generated later, every user story phase included test tasks that traced back to these requirements.

## Phase 2: Planning — Where the Cosmos DB Skill Earned Its Keep

I ran `/speckit.plan` and gave it a technology directive: Spring Boot 3, Azure Cosmos DB NoSQL API, Java 17+.

Before writing a single line of the plan, Copilot loaded the **Cosmos DB best-practices skill**. This isn't a generic "here are some tips" document. It's approximately 2,800 lines of structured rules, each with an ID, and each covering a specific aspect of Cosmos DB design. The agent cited specific rules in every decision it made.

### Partition Key Strategy

The dominant queries are "show me the global top 100 this week" and "show me the top 100 in Germany this week." The skill's `partition-hierarchical` and `partition-query-patterns` rules guided the decision to use a hierarchical partition key on the `leaderboard-entries` container: `/periodId` as level 1, `/region` as level 2.

This means:
- Regional leaderboard = single-partition query (fast, cheap)
- Global leaderboard = prefix query on `periodId` alone, scanning all regions within that week (bounded fan-out across ~200 country partitions, not unbounded)
- Historical queries = just change the `periodId` value

The alternatives were considered and rejected with citations. Single key on `/playerId`? Efficient for profile lookups but forces cross-partition scans for every leaderboard query — and leaderboard reads vastly outnumber profile lookups. That's `partition-query-patterns` saying no.

### Container Design

Three containers, separated by access pattern:

| Container | Partition Key | Access Pattern |
|-----------|--------------|----------------|
| `leaderboard-entries` | `/periodId`, `/region` | Very high read volume (leaderboard queries) |
| `scores` | `/playerId` | High write volume (~1M/day), append-only |
| `players` | `/playerId` | Low-latency point reads (player profiles) |

The skill's `model-denormalize-reads` rule justified the denormalized design — pre-computed aggregates on the player document, a separate materialized leaderboard entry per player per period. The `throughput-container-vs-database` rule justified separate containers so throughput can be scaled independently.

### Indexing

A composite index on `(bestScore DESC, scoreTimestamp ASC)` on `leaderboard-entries` — because the spec requires earlier submissions to rank higher when scores are tied. The `index-composite` and `index-composite-direction` rules from the skill drove this. Non-queried fields like `displayName` are excluded from indexing per `index-exclude-unused`.

### SDK Configuration

Gateway connection mode for the emulator (per `sdk-emulator-ssl`), Session consistency for read-your-writes guarantees (per `sdk-prefer-session-consistency`), `contentResponseOnWriteEnabled(true)` to get the document back after writes without a separate read (per `sdk-java-content-response`). The configuration class is named `CosmosDbConfig`, not `CosmosConfig` (per `sdk-java-cosmos-config`). Container setup uses `@Bean` dependency chains, never `@PostConstruct` (same rule).

Every decision in the plan traces to a specific rule ID. This isn't "the AI thought it was a good idea." This is "rule `partition-hierarchical` says to use hierarchical partition keys when your queries have a natural prefix hierarchy, and our leaderboard queries do."

The planning phase produced six artifacts: the implementation plan, a research document with 9 deep-dive sections, a Cosmos DB data model with document schemas and sample documents, REST API contracts with full request/response examples, a local development quickstart guide, and updated agent context. All before writing any application code.

## Phase 3: Task Breakdown — 50 Tasks, Tests Included

I ran `/speckit.tasks`. The initial pass produced 31 tasks. After I'd added the testing requirements to the spec, I regenerated: **50 tasks across 7 phases**.

The structure is key. Tasks aren't a flat list. They're organized by user story, with each story containing its own implementation tasks, unit tests, controller tests, and integration tests:

```
Phase 1: Setup (4 tasks) — Maven project, config files
Phase 2: Foundational (19 tasks) — All entities, repos, DTOs, error handling, test infra
Phase 3: US1 Score Submission (6 tasks) — Service, controller, 2 unit test tasks, 2 integration test tasks
Phase 4: US2 Global Leaderboard (6 tasks) — Same pattern
Phase 5: US3 Regional Leaderboard (8 tasks) — Same pattern
Phase 6: US4 Player Profile (5 tasks) — Same pattern
Phase 7: Polish (2 tasks) — Bean validation, pagination guards
```

The dependency chain is explicit. Phase 2 blocks everything. Within each user story, implementation comes first, then tests. Integration tests depend on the implementation being complete. Parallel tasks are marked `[P]` so the agent knows it can work on independent files simultaneously.

This is the part that matters: **the tests were designed as part of the task plan, not tacked on after implementation.** Each test task has a detailed description specifying exactly what to test — not "write tests for the score service" but "test submitScore happy path (new score persisted, leaderboard entry created, player stats updated, response contains rank), test higher-score-retained (submit 800 then 300 — bestScore stays 800), test player without region throws IllegalArgumentException."

## Phase 4: Analysis — Catching 19 Contradictions Before Writing Any Code

This is where SpecKit's `/speckit.analyze` command proved its value. Before implementation, the analysis agent cross-referenced all six artifacts — spec, plan, tasks, data model, contracts, and research — looking for inconsistencies.

### Round 1: 12 Findings

The two critical findings were contradictions hiding in the spec itself:

**FR-012** said "System MUST enforce display name constraints: 3–30 characters." But the assumptions section said "Display names are managed by the external player management system. The leaderboard API reads but does not manage them." These two statements can't both be true. The spec was telling the implementation to validate something it has no business validating.

**FR-014** said "System MUST authenticate all incoming requests." But another assumption said "Authentication is handled by the API gateway." Again, contradictory. If you'd coded to the requirements without reading the assumptions, you'd have built an auth layer that the system design says shouldn't exist.

Both requirements were struck through with explanatory notes. Ten more findings ranged from inconsistent region terminology (some places said "continental groupings," others said "country codes") to missing error codes in the API contracts.

### Round 2: 7 More Findings

After fixing all 12 from round 1, I ran the analysis again. It found 7 more:

- A contract header still referenced "validated" authentication after we'd changed the spec to say auth is handled by the gateway
- Stale comments in the data model that didn't match the updated spec
- A utility class (`PeriodUtil`) placed in Phase 7 that's actually a blocking dependency for Phase 3 — if you implemented in sequence, you'd hit a compilation error five phases too late
- The project constitution was still a placeholder

All fixed. The artifacts reached a consistent state. Only then did implementation begin.

**This is the argument for spec-driven development.** Nineteen inconsistencies caught and fixed before a single line of application code was written. How many of those would have become runtime bugs, failing tests with unclear causes, or API responses that don't match the documentation? In a traditional workflow, you'd find these one at a time during code review, QA, or production.

## Phase 5: Implementation — 50 Tasks, 38 Tests, One Build

I ran `/speckit.implement`. The agent executed all 50 tasks phase by phase: three controllers, three services, three repositories, three domain models, five DTOs, a global exception handler, a period utility, Cosmos DB configuration, and 38 tests across 6 test classes.

The build passed after four minor fixes — all of them the kind of thing that trips up any developer working with a new framework:

1. An `@Override` annotation on a method that doesn't actually override anything in the parent class
2. A `@MockBean` import pointing to a deprecated path in Spring Boot 3
3. A Mockito stub that could never be reached because validation throws before the mock is called
4. Jackson serializing a boolean `isNewBest` field as `newBest` because that's what it does with `is`-prefixed getters

These are framework-specific paper cuts. The business logic — the ranking, the score retention, the cross-container updates, the pagination — all worked on the first pass because the spec, plan, and analysis had already resolved the ambiguities.

## Phase 6: Runtime — Where Reality Meets the Plan

The application built. The tests passed. Time to start it against the actual Cosmos DB Emulator.

Two problems immediately surfaced, and neither was caught by any amount of specification analysis.

### Problem 1: SSL Certificate Trust

The Cosmos DB Emulator uses a self-signed SSL certificate. Java doesn't trust it by default. Spring Boot startup failed with `SunCertPathBuilderException`.

The quickstart guide (generated during planning) said to import the certificate into the JDK's truststore via `keytool`. That requires admin privileges on Windows when the JDK lives under `C:\Program Files\`. I didn't have admin.

The fix was to create a local truststore at the project root — copy the system `cacerts`, import the emulator certificate into the copy, and pass it to Spring Boot via JVM arguments. Not something any spec or plan would predict, but a standard Java-on-Windows headache.

### Problem 2: Cosmos DB Indexing Policy

After SSL was sorted, container creation failed with: *"The special mandatory indexing path '/' is not provided."*

The code set `ExcludedPaths` (to skip indexing on `displayName`) without explicitly setting `IncludedPaths`. In production Cosmos DB, this works fine — the default `/*` include path is implicit. The emulator, however, requires it to be explicit when you customize the policy at all.

One line fix:

```java
indexingPolicy.setIncludedPaths(List.of(new IncludedPath("/*")));
```

This is exactly the kind of emulator-vs-production behavior gap that the Cosmos DB skill's rules didn't cover — and arguably should. The `index-exclude-unused` rule tells you to exclude non-queried paths but doesn't mention that the emulator needs the root path restated when you do.

After both fixes, the app started. Three containers created. Gateway connection mode. Session consistency. Tomcat on port 8080.

## Phase 7: Live Verification — All Endpoints Confirmed

One more hurdle: the spec says PlayerProfile documents are "created and managed by the game's external player management system." There's no create-player endpoint in the API. To test anything, I needed to seed player data directly into the emulator.

I wrote a Python script that constructs HMAC-SHA256 auth headers for the Cosmos DB REST API and inserts three test players — one in the US, one in Germany, one in Japan. (An earlier attempt to run this as an inline PowerShell/Python one-liner failed spectacularly because PowerShell's string escaping mangled the JSON partition key header. Standalone script was the fix.)

With players seeded, the full flow worked:

- **Submit score** → 201, returns rank and `isNewBest: true`
- **Submit higher score** → best score updated, rank recalculated
- **Global leaderboard** → Three players, correctly ordered by best score descending
- **Regional leaderboard** → US query returns only the US player
- **Empty region** → Returns empty array, not an error
- **Player rank** → Correct rank with surrounding players
- **Player profile** → Accurate stats: 3 games played, correct average, current-week rank
- **Negative score** → 400 with `VALIDATION_ERROR`
- **Unknown player** → 404 with `PLAYER_NOT_FOUND`

Every response matched the API contracts from the planning phase.

## The Final Tally

| Metric | Count |
|--------|-------|
| Spec artifacts produced | 8 (spec, plan, research, data model, contracts, quickstart, tasks, checklist) |
| Analysis findings caught pre-implementation | 19 (across 2 rounds) |
| Tasks planned and executed | 50 |
| Source files generated | 20 (controllers, services, repos, models, DTOs, config, exception handling) |
| Test classes | 6 |
| Tests passing | 38/38 |
| Runtime issues requiring manual fix | 2 (SSL trust, indexing policy) |
| API endpoints verified against live emulator | 5 (13 test scenarios) |

## What I'd Do Differently

**The Cosmos DB skills should cover emulator-specific quirks.** The indexing policy issue — where the emulator requires explicit `IncludedPaths` but production doesn't — is a common gotcha. The skill's `index-exclude-unused` rule should have a note about this.

**Seed data should be part of the plan.** The spec correctly says players are externally managed, but the quickstart guide should have included a seed script or at least documented how to insert test data via the emulator's REST API. When your spec explicitly says "this entity comes from elsewhere," your dev setup needs a way to simulate "elsewhere."

**SSL truststore creation should be automated.** The quickstart guide assumed admin access for `keytool -import`. A more robust approach would be to generate the local truststore as part of the project setup and include the JVM argument in a Maven profile.

## The Takeaway

The value of spec-driven development with AI isn't that it writes code faster. It's that it **resolves contradictions before they become bugs.** Nineteen findings caught during analysis. Four minor fixes during build. Two runtime issues from emulator-specific behavior. Zero logic bugs.

The tests weren't an afterthought — they were requirements. The partition key strategy wasn't a guess — it was a rule-referenced decision. The API contracts weren't aspirational — they were verified against live responses.

If you're building with AI agents, give them structure. Give them specs. Give them analysis passes. The code will be better, and you'll spend your debugging time on SSL certificates instead of business logic.

---

*Built with [SpecKit](https://github.com/speckit), [GitHub Copilot](https://github.com/features/copilot), and the [Azure Cosmos DB Best Practices Skill](https://github.com/AzureCosmosDB/cosmosdb-agent-kit). The complete spec artifacts, source code, and test suite are in the `specs/001-game-leaderboard-api/` directory.*
