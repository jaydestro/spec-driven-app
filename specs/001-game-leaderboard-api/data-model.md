# Data Model: Game Leaderboard API

**Feature**: 001-game-leaderboard-api
**Date**: 2026-03-30
**Storage**: Azure Cosmos DB NoSQL API

## Cosmos DB Account Configuration

| Setting | Value | Rationale |
|---------|-------|-----------|
| API | NoSQL | Specified by requirements |
| Consistency Level | Session | Guarantees read-your-writes for individual players after score submission |
| Capacity Mode | Autoscale (local dev: serverless/manual 400 RU) | Variable workload with peak periods (`throughput-autoscale`) |

## Containers

### 1. `leaderboard-entries`

**Purpose**: Denormalized leaderboard ranking data. Primary read target for all leaderboard queries.

| Setting | Value |
|---------|-------|
| Partition Key | Hierarchical: `/periodId`, `/region` |
| Autoscale Max RU | 10,000 (production estimate; 400 manual for local dev) |
| TTL | -1 (no expiry — historical retention) |

**Composite Indexes**:
```json
{
  "compositeIndexes": [
    [
      { "path": "/bestScore", "order": "descending" },
      { "path": "/scoreTimestamp", "order": "ascending" }
    ]
  ]
}
```

**Excluded Index Paths**: `/displayName/?`, `/avatarUrl/?`

**Document Schema**:

```java
@Container(containerName = "leaderboard-entries")
public class LeaderboardEntry {

    @Id
    @GeneratedValue
    private String id;                    // "{periodId}_{playerId}"

    @PartitionKey
    private String periodId;              // e.g., "2026-W13" (ISO week) — HPK level 1

    // Hierarchical partition key level 2 — configure via CosmosDbConfig, not annotated here
    // (Spring Data Azure Cosmos DB HPK requires programmatic container config in T005)
    private String region;                // e.g., "US", "DE", "JP" (ISO 3166-1 alpha-2)

    private String playerId;              // Reference to players container

    private String displayName;           // Denormalized from player profile

    private long bestScore;               // Highest score this period

    private String scoreTimestamp;        // ISO 8601 timestamp of the best score (tiebreaker)

    private String type;                  // "leaderboardEntry" (type discriminator)

    private int schemaVersion;            // 1 (schema versioning per model-schema-versioning)
}
```

**Sample Document**:
```json
{
  "id": "2026-W13_player-abc123",
  "periodId": "2026-W13",
  "region": "US",
  "playerId": "player-abc123",
  "displayName": "ProGamer42",
  "bestScore": 98500,
  "scoreTimestamp": "2026-03-28T14:30:00Z",
  "type": "leaderboardEntry",
  "schemaVersion": 1
}
```

---

### 2. `scores`

**Purpose**: Immutable record of every individual score submission. Write-heavy, rarely read directly.

| Setting | Value |
|---------|-------|
| Partition Key | `/playerId` |
| Autoscale Max RU | 4,000 (production estimate; 400 manual for local dev) |
| TTL | -1 (no expiry) |

**Document Schema**:

```java
@Container(containerName = "scores")
public class ScoreEntry {

    @Id
    @GeneratedValue
    private String id;                    // Auto-generated UUID

    @PartitionKey
    private String playerId;              // Player who submitted

    private long score;                   // Score value (0–999,999,999)

    private String submittedAt;           // ISO 8601 timestamp

    private String periodId;              // Week this score belongs to

    private String region;                // Player's region at submission time

    private String type;                  // "scoreEntry"

    private int schemaVersion;            // 1
}
```

**Sample Document**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "playerId": "player-abc123",
  "score": 98500,
  "submittedAt": "2026-03-28T14:30:00Z",
  "periodId": "2026-W13",
  "region": "US",
  "type": "scoreEntry",
  "schemaVersion": 1
}
```

---

### 3. `players`

**Purpose**: Player profiles with pre-computed aggregate statistics. Read via point reads (1 RU per read).

| Setting | Value |
|---------|-------|
| Partition Key | `/playerId` |
| Autoscale Max RU | 2,000 (production estimate; 400 manual for local dev) |
| TTL | -1 (no expiry) |

**Document Schema**:

```java
@Container(containerName = "players")
public class PlayerProfile {

    @Id
    @GeneratedValue
    private String id;                    // Same as playerId

    @PartitionKey
    private String playerId;

    private String displayName;           // Managed by external player system; stored as-is

    private String region;                // ISO 3166-1 alpha-2 country code

    private long bestScore;               // All-time highest score (pre-computed)

    private int totalGamesPlayed;         // Cumulative count (pre-computed aggregate)

    private double averageScore;          // Running average (pre-computed aggregate)

    private long totalScoreSum;           // Sum of all scores (used to recalculate average)

    private String registeredAt;          // ISO 8601 timestamp

    private String lastUpdated;           // ISO 8601 timestamp (staleness detection per model-denormalize-reads)

    private String type;                  // "playerProfile"

    private int schemaVersion;            // 1
}
```

**Sample Document**:
```json
{
  "id": "player-abc123",
  "playerId": "player-abc123",
  "displayName": "ProGamer42",
  "region": "US",
  "bestScore": 98500,
  "totalGamesPlayed": 347,
  "averageScore": 42150.5,
  "totalScoreSum": 14626223,
  "registeredAt": "2026-01-15T08:00:00Z",
  "lastUpdated": "2026-03-28T14:30:00Z",
  "type": "playerProfile",
  "schemaVersion": 1
}
```
