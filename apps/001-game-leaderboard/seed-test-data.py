import base64, hashlib, hmac, json, urllib.request, urllib.parse, ssl, datetime

key = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="
endpoint = "https://localhost:8081"
db = "leaderboard-db"

def get_auth(verb, resource_type, resource_link, date):
    decoded = base64.b64decode(key)
    text = f"{verb.lower()}\n{resource_type.lower()}\n{resource_link}\n{date.lower()}\n\n"
    sig = base64.b64encode(hmac.new(decoded, text.encode("utf-8"), hashlib.sha256).digest()).decode("utf-8")
    return urllib.parse.quote(f"type=master&ver=1.0&sig={sig}")

def insert_doc(container, partition_key_value, doc):
    date = datetime.datetime.utcnow().strftime("%a, %d %b %Y %H:%M:%S GMT")
    resource_link = f"dbs/{db}/colls/{container}"
    auth = get_auth("post", "docs", resource_link, date)

    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    url = f"{endpoint}/{resource_link}/docs"
    data = json.dumps(doc).encode()
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Authorization", auth)
    req.add_header("x-ms-date", date)
    req.add_header("x-ms-version", "2018-12-31")
    req.add_header("Content-Type", "application/json")
    req.add_header("x-ms-documentdb-partitionkey", json.dumps([partition_key_value]))

    try:
        resp = urllib.request.urlopen(req, context=ctx)
        print(f"  Created {doc['id']} -> {resp.status}")
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        if e.code == 409:
            print(f"  {doc['id']} already exists (409)")
        else:
            print(f"  Error {e.code}: {body}")

# Seed players
players = [
    {"id": "player-001", "playerId": "player-001", "displayName": "TestGamer42", "region": "US",
     "bestScore": 0, "totalGamesPlayed": 0, "averageScore": 0.0, "totalScoreSum": 0,
     "registeredAt": "2026-01-15T08:00:00Z", "lastUpdated": "2026-01-15T08:00:00Z",
     "type": "playerProfile", "schemaVersion": 1},
    {"id": "player-002", "playerId": "player-002", "displayName": "ProSniper99", "region": "DE",
     "bestScore": 0, "totalGamesPlayed": 0, "averageScore": 0.0, "totalScoreSum": 0,
     "registeredAt": "2026-02-20T10:00:00Z", "lastUpdated": "2026-02-20T10:00:00Z",
     "type": "playerProfile", "schemaVersion": 1},
    {"id": "player-003", "playerId": "player-003", "displayName": "NinjaWarrior", "region": "JP",
     "bestScore": 0, "totalGamesPlayed": 0, "averageScore": 0.0, "totalScoreSum": 0,
     "registeredAt": "2026-03-01T12:00:00Z", "lastUpdated": "2026-03-01T12:00:00Z",
     "type": "playerProfile", "schemaVersion": 1},
]

print("Seeding players...")
for p in players:
    insert_doc("players", p["playerId"], p)

print("Done! Players seeded.")
