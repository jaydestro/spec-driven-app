# How a Spec-Driven Workflow and the Cosmos DB Agent Kit Steered an AI Agent to Build a Production-Ready API

I gave an AI agent a single sentence — *"Build an API for a mobile game's leaderboard system"* — and walked away with a fully implemented Spring Boot API backed by Azure Cosmos DB, 38 passing tests, and a running application verified against real API calls.

But the interesting part isn't that the AI wrote code. Any AI agent can generate code. The interesting part is what happened *before* it wrote code — and specifically, how the **Azure Cosmos DB Agent Kit** turned what would have been a generic, probably-wrong database design into a properly partitioned, correctly indexed, rule-referenced architecture that worked on the first try.

Here's the problem with asking an AI to "build a leaderboard API with Cosmos DB." Without guardrails, the agent will pick a partition key that makes writes easy but queries expensive. It'll put everything in one container. It'll use the default indexing policy and wonder why queries cost 50 RU when they should cost 5. It'll use `@PostConstruct` for initialization because that's what most blog posts show. The code will compile. The tests will pass against mocks. And the moment you point it at a real database with real data volumes, the design falls apart.

That's the gap this workflow filled. **SpecKit** gave the agent a structured process — spec, plan, analyze, implement. The **Cosmos DB best-practices skill** from the agent kit gave it 2,800 lines of domain-specific rules to follow during planning. And the combination meant the agent didn't just produce code that works — it produced a Cosmos DB design where every decision traces to a specific rule ID, not to whatever the model's training data happened to reinforce.

This is what spec-driven development looks like when you pair it with domain-specific tooling.

## The Stack

- **SpecKit v0.4.3** — A spec-driven development framework that runs inside VS Code. It provides structured prompts for each phase: `/speckit.specify`, `/speckit.plan`, `/speckit.tasks`, `/speckit.analyze`, and `/speckit.implement`. Each prompt enforces a specific output format and quality gate before the next phase can begin.
- **GitHub Copilot Chat (Claude Opus 4.6)** — The AI coding agent driving every phase. Not just autocomplete — this is an agentic workflow where Copilot Chat reads files, makes decisions, writes code, runs builds, and debugs failures.
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

## Phase 2: Planning — Where the Agent Kit Changed Everything

This is the phase that made the biggest difference. I ran `/speckit.plan` and gave it a technology directive: Spring Boot 3, Azure Cosmos DB NoSQL API, Java 17+.

Before writing a single line of the plan, Copilot Chat loaded the **Cosmos DB best-practices skill** from the [Azure Cosmos DB Agent Kit](https://github.com/AzureCosmosDB/cosmosdb-agent-kit). This isn't a generic "here are some tips" document. It's approximately 2,800 lines of structured rules, each with an ID, and each covering a specific aspect of Cosmos DB design. The agent cited specific rules in every decision it made.

**This is the part that matters most in this entire post.** Without the agent kit, the AI would have generated a plan based on its general training data — which is a mix of good practices, outdated blog posts, and Stack Overflow answers of varying quality. With the agent kit, the AI had a curated, opinionated set of rules written by the Cosmos DB team. The difference is the difference between "this might work" and "this is how it should be done."

Let me walk through the specific decisions the agent kit steered.

### Partition Key Strategy — The Decision That Shapes Everything

In Cosmos DB, your partition key choice determines your query performance, your throughput distribution, and your cost. Get it wrong and you're redesigning your data model. Get it right and everything else follows.

The dominant queries for a leaderboard are "show me the global top 100 this week" and "show me the top 100 in Germany this week." Without the agent kit, an AI would likely pick `/playerId` as the partition key — it's the obvious primary identifier, and most Cosmos DB tutorials use the entity's natural key. But that choice means every leaderboard query is a cross-partition fan-out across potentially millions of partitions. At scale, that's slow and expensive.

The skill's `partition-hierarchical` and `partition-query-patterns` rules steered the agent to a hierarchical partition key on the `leaderboard-entries` container: `/periodId` as level 1, `/region` as level 2. This changes everything:

- **Regional leaderboard** = single-partition query. One physical partition, one round trip. Fast and cheap.
- **Global leaderboard** = prefix query on `periodId` alone. The query fans out across all region partitions within that week — roughly 200 countries — but that's bounded fan-out, not unbounded. The data for last week is in completely separate partitions that aren't touched.
- **Historical queries** = just change the `periodId`. Old leaderboard periods naturally land in isolation without any cleanup jobs or TTL management.

The agent documented the alternatives it considered and why they were rejected, citing specific rules:

> Single key on `/playerId`? Efficient for profile lookups but forces cross-partition scans for every leaderboard query — and leaderboard reads vastly outnumber profile lookups. That's `partition-query-patterns` saying no.

> Synthetic key `/periodId_region`? Works but loses the hierarchical prefix query benefit. Requires exact match on both.

This wasn't the agent winging it. This was the agent applying a decision framework from the skill, considering the access patterns from the spec, and arriving at the right answer with justification. That's what domain-specific rules buy you.

### Multi-Container Design — Separating by Access Pattern

Without guidance, an AI agent would likely put all document types in a single container with a type discriminator. It's simpler. But the skill's `model-denormalize-reads` and `throughput-container-vs-database` rules pushed toward three separate containers:

| Container | Partition Key | Why It's Separate |
|-----------|--------------|-------------------|
| `leaderboard-entries` | `/periodId`, `/region` | Very high read volume. Optimized for leaderboard queries. Throughput scaled for reads. |
| `scores` | `/playerId` | High write volume (~1M/day). Append-only log. Throughput scaled for writes. |
| `players` | `/playerId` | Low-latency point reads only. Small documents. 1 RU per read. |

The key insight from the skill: when access patterns diverge significantly, separate containers let you scale throughput independently. The leaderboard container needs read throughput. The scores container needs write throughput. Putting them together means paying for the maximum of both on every operation.

The agent also applied the `model-denormalize-reads` rule — instead of normalizing data and joining at query time (which Cosmos DB doesn't support efficiently), the design pre-computes aggregates. The player document stores `totalGamesPlayed`, `averageScore`, and `bestScore` directly. The leaderboard entry duplicates the player's `displayName` and `region`. More storage, but leaderboard queries never touch the players container.

### Composite Indexes — Tiebreaker Ordering

The spec requires that when two players have the same score, the one who submitted earlier ranks higher. In Cosmos DB, this means a composite index: `(bestScore DESC, scoreTimestamp ASC)`.

The skill's `index-composite` and `index-composite-direction` rules didn't just say "use a composite index." They specified that the sort directions matter — the direction in the index must match the direction in the ORDER BY clause. DESC for score (highest first), ASC for timestamp (earliest first). Get the direction wrong and Cosmos DB ignores the index and does a full scan.

The `index-exclude-unused` rule also prompted the agent to explicitly exclude non-queried fields from the index. `displayName` is returned in responses but never filtered or sorted on — so it's excluded, reducing write RU cost on every score submission.

### SDK Configuration — The Stuff That Doesn't Show Up in Tutorials

This is where the agent kit's value is most subtle. These aren't architecture decisions — they're configuration choices that tutorials skip but production deployments need:

- **Gateway connection mode for the emulator** (rule `sdk-emulator-ssl`). Direct mode is faster in production, but the emulator doesn't support it reliably. Without this rule, the agent would have used Direct mode, and the first runtime error would have been confusing.
- **Session consistency** (rule `sdk-prefer-session-consistency`). The default for most tutorials is "Eventual," but the agent kit says to use Session for read-your-writes guarantees. When a player submits a score and immediately views the leaderboard, they need to see their own submission. Session consistency guarantees that.
- **`contentResponseOnWriteEnabled(true)`** (rule `sdk-java-content-response`). This returns the document in the write response so you don't need a separate read after each write. Saves 1 RU per write operation.
- **Config class named `CosmosDbConfig`, not `CosmosConfig`** (rule `sdk-java-cosmos-config`). This avoids a name collision with an internal Spring class. A subtle gotcha that would cause hard-to-diagnose startup failures.
- **`@Bean` dependency chains, never `@PostConstruct`** (same rule). Spring lifecycle ordering with `@PostConstruct` is unreliable for Cosmos DB initialization. The skill explicitly says to use `@Bean` methods so Spring manages the dependency graph.

None of these would appear in a generic AI-generated plan. Each one would have become a runtime bug discovered during testing. The agent kit front-loaded them into the planning phase.

### What the Plan Produced

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

The value of spec-driven development with AI isn't that it writes code faster. It's that it **makes the right decisions in the planning phase, before any code exists.**

The Cosmos DB Agent Kit is the proof. Without it, the agent would have produced a working API — it would have compiled, passed basic tests, and returned JSON. But the partition key would have been wrong, the container design would have been naive, the indexing policy would have been default, and the SDK configuration would have been cargo-culted from a tutorial. You'd discover all of this in production, under load, when it's expensive to fix.

With the agent kit loaded, every database design decision in the plan traces to a specific rule ID. The partition key strategy references `partition-hierarchical` and `partition-query-patterns`. The container separation cites `throughput-container-vs-database`. The composite index directions follow `index-composite-direction`. The SDK configuration applies `sdk-java-cosmos-config` and `sdk-prefer-session-consistency`. These aren't suggestions the agent might follow — they're constraints that shaped every artifact downstream.

The spec-driven workflow amplified this further. SpecKit's analysis passes caught 19 inconsistencies between the spec, the plan, and the task list before a single line of Java was written. The agent kit ensured the database design was right. The spec framework ensured everything else was consistent with that design. Together, they produced 50 tasks, 38 passing tests, and 5 verified API endpoints with zero logic bugs.

If you're building with AI agents, don't just give them a prompt and hope for the best. Give them domain-specific rules so they make the right architectural decisions. Give them a spec framework so those decisions propagate consistently through the entire codebase. The code will be better, and you'll spend your debugging time on SSL certificates instead of partition key redesigns.

---

*Built with [SpecKit](https://github.com/speckit), [GitHub Copilot Chat](https://github.com/features/copilot) (Claude Opus 4.6), and the [Azure Cosmos DB Best Practices Skill](https://github.com/AzureCosmosDB/cosmosdb-agent-kit). The complete spec artifacts, source code, and test suite are in the `specs/001-game-leaderboard-api/` and `apps/001-game-leaderboard/` directories.*
