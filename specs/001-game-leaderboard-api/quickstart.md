# Quickstart: Game Leaderboard API

**Feature**: 001-game-leaderboard-api
**Date**: 2026-03-30

## Prerequisites

- **Java 17+** (required for Spring Boot 3.x)
- **Maven 3.8+**
- **Azure Cosmos DB Emulator** (Windows) or Docker vNext Linux emulator

### Verify Java

```powershell
java -version
# Should show 17.x or higher

# If not, set JAVA_HOME:
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

## Step 1: Start Cosmos DB Emulator

### Windows Emulator (Recommended)

```powershell
# Start from Start Menu or command line:
& "$env:ProgramFiles\Azure Cosmos DB Emulator\Microsoft.Azure.Cosmos.Emulator.exe"

# Verify it's running:
curl -sk https://localhost:8081/_explorer/emulator.pem
```

### Docker vNext (Alternative)

```powershell
docker pull mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview

docker run --detach --publish 8081:8081 --publish 1234:1234 `
  --name cosmosdb-emulator `
  mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview `
  --protocol https
```

## Step 2: Import Emulator SSL Certificate (Java)

The emulator uses a self-signed certificate. Import it into the JDK truststore:

```powershell
# Download the emulator certificate
curl -sk https://localhost:8081/_explorer/emulator.pem -o cosmos-emulator.pem

# Import into JDK truststore
keytool -import -alias cosmosdb-emulator -keystore "$env:JAVA_HOME\lib\security\cacerts" `
  -file cosmos-emulator.pem -storepass changeit -noprompt
```

## Step 3: Build and Run

```powershell
# From the project root
mvn clean install -DskipTests

# Run with emulator profile
mvn spring-boot:run -Dspring-boot.run.profiles=emulator
```

The API starts on `http://localhost:8080`.

## Step 4: Verify

### Create a test player (seed data)

The API does not manage player registration — seed a player document directly via the Cosmos DB Data Explorer at `https://localhost:8081/_explorer/index.html`:

Navigate to `players` container and create:
```json
{
  "id": "player-test001",
  "playerId": "player-test001",
  "displayName": "TestPlayer",
  "region": "US",
  "bestScore": 0,
  "totalGamesPlayed": 0,
  "averageScore": 0.0,
  "totalScoreSum": 0,
  "registeredAt": "2026-03-30T00:00:00Z",
  "lastUpdated": "2026-03-30T00:00:00Z",
  "type": "playerProfile",
  "schemaVersion": 1
}
```

### Submit a score

```powershell
curl -X POST http://localhost:8080/api/scores `
  -H "Content-Type: application/json" `
  -d '{"playerId": "player-test001", "score": 75000}'
```

### Get global leaderboard

```powershell
curl http://localhost:8080/api/leaderboards/global
```

### Get player profile

```powershell
curl http://localhost:8080/api/players/player-test001
```

## Configuration

### application.yml (default)

```yaml
azure:
  cosmos:
    endpoint: ${COSMOS_ENDPOINT:https://localhost:8081}
    key: ${COSMOS_KEY:C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==}
    database: leaderboard-db
    populate-query-metrics: true
```

### application-emulator.yml

```yaml
azure:
  cosmos:
    endpoint: https://localhost:8081
    key: C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==
    # Gateway mode is required for the emulator (sdk-emulator-ssl)
    connection-mode: gateway
```

## Emulator Reset (Between Test Runs)

Delete all databases via the Data Explorer at `https://localhost:8081/_explorer/index.html`, or:

```powershell
# Verify clean state
curl -sk https://localhost:8081/dbs
# Should return: {"_count": 0}
```
