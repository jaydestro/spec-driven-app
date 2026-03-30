# REST API Contract: Game Leaderboard API

**Base URL**: `http://localhost:8080/api`
**Content-Type**: `application/json`
**Authentication**: Handled by API gateway. Requests arrive with a verified player identity token — this API does not validate or issue tokens.

---

## 1. Submit Score

**POST** `/api/scores`

Submit a new game score for a player.

### Request Body

```json
{
  "playerId": "player-abc123",
  "score": 98500
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `playerId` | string | Yes | Must exist in players container |
| `score` | long | Yes | 0 ≤ score ≤ 999,999,999 |

### Response: 201 Created

```json
{
  "playerId": "player-abc123",
  "score": 98500,
  "isNewBest": true,
  "periodId": "2026-W13",
  "globalRank": 42,
  "regionalRank": 12,
  "region": "US"
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Score out of range, missing required fields, or player has no region assigned |
| 404 | Player not found |

---

## 2. Get Global Leaderboard

**GET** `/api/leaderboards/global`

Retrieve the global leaderboard for the current (or specified) week.

### Query Parameters

| Parameter | Type | Default | Constraints |
|-----------|------|---------|-------------|
| `periodId` | string | Current week | ISO 8601 week `YYYY-Www` |
| `page` | int | 1 | ≥ 1 |
| `pageSize` | int | 50 | 1–100 |

### Response: 200 OK

```json
{
  "periodId": "2026-W13",
  "scope": "global",
  "page": 1,
  "pageSize": 50,
  "totalEntries": 487293,
  "entries": [
    {
      "rank": 1,
      "playerId": "player-xyz789",
      "displayName": "TopPlayer",
      "bestScore": 999100,
      "region": "KR",
      "scoreTimestamp": "2026-03-25T09:15:00Z"
    },
    {
      "rank": 2,
      "playerId": "player-def456",
      "displayName": "EliteGamer",
      "bestScore": 998700,
      "region": "US",
      "scoreTimestamp": "2026-03-26T11:20:00Z"
    }
  ]
}
```

---

## 3. Get Regional Leaderboard

**GET** `/api/leaderboards/regions/{region}`

Retrieve the leaderboard for a specific country/region.

### Path Parameters

| Parameter | Type | Constraints |
|-----------|------|-------------|
| `region` | string | ISO 3166-1 alpha-2 code (e.g., `US`, `DE`, `JP`) |

### Query Parameters

| Parameter | Type | Default | Constraints |
|-----------|------|---------|-------------|
| `periodId` | string | Current week | ISO 8601 week `YYYY-Www` |
| `page` | int | 1 | ≥ 1 |
| `pageSize` | int | 50 | 1–100 |

### Response: 200 OK

Same shape as global leaderboard response, with `"scope": "regional"` and entries filtered to the specified region.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Invalid region code |
| 200 | Valid region with no entries returns empty `entries` array |

---

## 4. Get Player Rank

**GET** `/api/leaderboards/players/{playerId}/rank`

Get a player's rank and the surrounding 10 players above and below.

### Path Parameters

| Parameter | Type | Constraints |
|-----------|------|-------------|
| `playerId` | string | Must exist |

### Query Parameters

| Parameter | Type | Default | Constraints |
|-----------|------|---------|-------------|
| `periodId` | string | Current week | ISO 8601 week `YYYY-Www` |
| `scope` | string | `global` | `global` or `regional` |
| `surrounding` | int | 10 | 1–50 |

### Response: 200 OK

```json
{
  "playerId": "player-abc123",
  "displayName": "ProGamer42",
  "bestScore": 98500,
  "rank": 5432,
  "scope": "global",
  "periodId": "2026-W13",
  "surroundingPlayers": [
    {
      "rank": 5422,
      "playerId": "player-111",
      "displayName": "NearbyA",
      "bestScore": 98520,
      "region": "DE"
    },
    {
      "rank": 5432,
      "playerId": "player-abc123",
      "displayName": "ProGamer42",
      "bestScore": 98500,
      "region": "US"
    },
    {
      "rank": 5442,
      "playerId": "player-222",
      "displayName": "NearbyB",
      "bestScore": 98480,
      "region": "JP"
    }
  ]
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 404 | Player not found or player has no entries in the specified period |

---

## 5. Get Player Profile

**GET** `/api/players/{playerId}`

Retrieve a player's profile with cumulative statistics.

### Path Parameters

| Parameter | Type | Constraints |
|-----------|------|-------------|
| `playerId` | string | Must exist |

### Response: 200 OK

```json
{
  "playerId": "player-abc123",
  "displayName": "ProGamer42",
  "region": "US",
  "bestScore": 98500,
  "totalGamesPlayed": 347,
  "averageScore": 42150.5,
  "registeredAt": "2026-01-15T08:00:00Z",
  "currentWeekRank": {
    "global": 5432,
    "regional": 1204,
    "periodId": "2026-W13"
  }
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 404 | Player not found |

---

## Common Error Response Format

```json
{
  "error": "PLAYER_NOT_FOUND",
  "message": "Player with ID 'player-abc123' does not exist",
  "timestamp": "2026-03-28T14:30:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `error` | string | Machine-readable error code |
| `message` | string | Human-readable description |
| `timestamp` | string | ISO 8601 timestamp of the error |
