# Research: Game Leaderboard API

**Feature**: 001-game-leaderboard-api
**Date**: 2026-03-30

## 1. Cosmos DB Partition Key Strategy for Leaderboards

### Decision: Use hierarchical partition keys with leaderboard period + region

**Rationale**: The dominant query patterns are:
- Global top 100 for a given week → partition by `periodId` (e.g., `"2026-W13"`)
- Regional top 100 for a given week → partition by `periodId` + `region`
- Player rank within a period → requires knowing the period

A hierarchical partition key `/periodId` → `/region` enables:
- Global leaderboard: prefix query on `periodId` alone (scans all regions within that period — bounded fan-out across ~200 country partitions)
- Regional leaderboard: full key query on `periodId` + `region` (single-partition, <50ms)
- Historical queries: different `periodId` values isolate weekly data

**Alternatives considered**:
- Single key `/playerId`: Efficient for player profile lookups but forces cross-partition scans for all leaderboard queries. Rejected — leaderboard reads vastly outnumber player lookups.
- Single key `/region`: Good for regional queries but global leaderboard still requires cross-partition. Also conflates all time periods into one partition.
- Synthetic key `/periodId_region`: Works but loses the hierarchical prefix query benefit. Requires exact match on both.

**Cosmos DB rules applied**: `partition-hierarchical`, `partition-query-patterns`, `partition-high-cardinality`, `partition-avoid-hotspots`

## 2. Leaderboard Container Design (Multi-Container Strategy)

### Decision: 3 containers with distinct access patterns

| Container | Partition Key | Purpose | Primary Access |
|-----------|--------------|---------|----------------|
| `leaderboard-entries` | Hierarchical: `/periodId`, `/region` | Denormalized ranking entries (one per player per period per region) | Leaderboard queries (high read) |
| `scores` | `/playerId` | Individual score submission records | Score writes, player score history |
| `players` | `/playerId` | Player profiles with pre-computed aggregates | Player profile lookups |

**Rationale**: Separating containers by access pattern allows independent throughput scaling. The `leaderboard-entries` container handles the very high read volume with autoscale, while `scores` absorbs the ~1M daily writes. The `players` container serves low-latency profile lookups as point reads.

**Alternatives considered**:
- Single container with type discriminator: Simpler to manage but partition key must serve all patterns. At 500K players the partition key trade-offs become untenable for both leaderboard reads and score writes.
- Two containers (merge players into scores): Possible, but player profiles are read far more often than individual scores. Separating them allows the `players` container to use smaller documents and point reads (1 RU per read).

**Cosmos DB rules applied**: `model-type-discriminator`, `model-denormalize-reads`, `throughput-container-vs-database`, `pattern-change-feed-materialized-views`

## 3. Ranking Strategy

### Decision: COUNT-based rank queries for on-demand rank, pre-sorted leaderboard entries for top-N

**Rationale**: Two complementary approaches per `pattern-efficient-ranking`:

1. **Top-N queries** (global/regional top 100): Query `leaderboard-entries` with `ORDER BY bestScore DESC` + `TOP 100` within a partition. This is a single-partition sorted read — efficient with a composite index on `(bestScore DESC, scoreTimestamp ASC)`.

2. **Player rank lookup**: Use a `COUNT` query — `SELECT VALUE COUNT(1) FROM c WHERE c.bestScore > @playerScore` within the partition. This returns rank in ~3-5 RU regardless of partition size. Combine with a `TOP 21` query centered on the player's score to get surrounding players (±10).

3. **Weekly leaderboard periods**: `periodId` (e.g., `"2026-W13"`) is the first level of the hierarchical partition key. When a new week starts, new entries are written to a new `periodId`. Old periods remain queryable for historical views.

**Alternatives considered**:
- Pre-computed ranks via Change Feed: Better for extremely high read throughput but adds infrastructure complexity (Change Feed processor, lease container, rank recomputation jobs). Deferred to a future optimization if COUNT queries become a bottleneck.
- Score buckets for approximate ranking: Not acceptable for this use case — players expect exact ranks.

**Cosmos DB rules applied**: `pattern-efficient-ranking`, `query-avoid-cross-partition`, `index-composite`

## 4. Score Submission Flow

### Decision: Write score to `scores` container, conditionally update `leaderboard-entries` and `players`

**Flow**:
1. Validate score (range, region, player exists)
2. Write score document to `scores` container (partition key: `playerId`)
3. Read current `leaderboard-entries` document for this player + period + region
4. If new score > existing best score: upsert `leaderboard-entries` with new best score
5. Update `players` document: increment `totalGamesPlayed`, update `bestScore` if higher, recalculate `averageScore`
6. Return updated rank via COUNT query

Steps 3-5 are not transactional across containers (Cosmos DB transactions are single-partition only). Accepted trade-off: in rare race conditions, stats may be momentarily inconsistent. The next score submission self-corrects.

**Cosmos DB rules applied**: `sdk-etag-concurrency`, `model-denormalize-reads` (pre-computed aggregates on `players`)

## 5. Weekly Period Management

### Decision: Period ID format `YYYY-Www` (ISO 8601 week), calculated at score submission time

**Rationale**: The current week's `periodId` is computed from the score's timestamp (e.g., `2026-W13`). No background job needed to "rotate" leaderboards — new entries naturally land in new partitions. Historical periods are retained indefinitely (or pruned by a TTL policy in the future).

The `leaderboard-entries` container includes a `periodId` field as the first hierarchical partition key level. Querying any historical week is identical to querying the current week — just change the `periodId` value.

**Alternatives considered**:
- Time-based TTL on old entries: Not suitable — requirement is to keep historical data.
- Separate container per week: Over-engineering; hierarchical partition keys handle this elegantly.

## 6. Spring Data Cosmos vs. Raw SDK

### Decision: Use Spring Data Cosmos repositories with custom `@Query` annotations for complex queries

**Rationale**: Spring Data Cosmos provides the repository abstraction with derived query methods and custom SQL queries. For the top-N and COUNT queries required by the leaderboard, custom `@Query` annotations with Cosmos SQL give full control. Spring Data handles entity mapping, partition key routing, and connection lifecycle.

**Key implementation rules** (from skill):
- Annotate entities with `@Container`, `@PartitionKey`, `@Id` + `@GeneratedValue` — not JPA annotations (`sdk-spring-data-annotations`)
- IDs must be `String` type
- Use `CosmosRepository<Entity, String>`, not `JpaRepository` (`sdk-spring-data-repository`)
- Configuration class must NOT be named `CosmosConfig` — use `CosmosDbConfig` (`sdk-java-cosmos-config`)
- Use `@Bean` dependency chain, never `@PostConstruct` for initialization (`sdk-java-cosmos-config`)
- Use Gateway connection mode for emulator, Direct for production (`sdk-emulator-ssl`)
- Enable `contentResponseOnWriteEnabled(true)` for write operations (`sdk-java-content-response`)

## 7. Indexing Strategy

### Decision: Composite indexes on leaderboard-entries, exclude unused paths

| Container | Composite Index | Purpose |
|-----------|----------------|---------|
| `leaderboard-entries` | `(bestScore DESC, scoreTimestamp ASC)` | Top-N queries with tiebreaker |
| `leaderboard-entries` | `(region ASC, bestScore DESC)` | Regional leaderboard sort |
| `scores` | None needed | Writes only, queried by point read or playerId |
| `players` | None needed | Point reads by playerId |

Exclude paths not used in queries (e.g., `displayName`, `avatarUrl`) from indexing on `leaderboard-entries` to reduce write RU cost.

**Cosmos DB rules applied**: `index-composite`, `index-composite-direction`, `index-exclude-unused`

## 8. Spec Amendment: Weekly Periods

The original spec (created by `/speckit.specify`) states "The system does not need to support historical leaderboards or season-based resets in this version." The plan input explicitly requires weekly leaderboard periods with historical data retention. **The plan input takes precedence** as a refinement of the spec. The data model and partition strategy account for weekly periods.

## 9. Local Development: Cosmos DB Emulator

### Decision: Azure Cosmos DB Windows Emulator with Gateway connection mode

**Configuration**:
- Endpoint: `https://localhost:8081`
- Key: Well-known emulator key
- Connection mode: Gateway (required for emulator — `sdk-emulator-ssl`)
- SSL: Import emulator certificate to JDK truststore or use Spring profile to disable SSL verification
- Spring profile: `emulator` activates `application-emulator.yml` with emulator-specific settings

**Cosmos DB rules applied**: `sdk-emulator-ssl`, `tooling-emulator-setup`, `sdk-local-dev-config`
