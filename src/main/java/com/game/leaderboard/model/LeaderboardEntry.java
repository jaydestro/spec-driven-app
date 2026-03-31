package com.game.leaderboard.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;

@Container(containerName = "leaderboard-entries", autoCreateContainer = false)
public class LeaderboardEntry {

    @Id
    private String id;

    @PartitionKey
    private String periodId;

    private String region;

    private String playerId;

    private String displayName;

    private long bestScore;

    private String scoreTimestamp;

    private String type = "leaderboardEntry";

    private int schemaVersion = 1;

    public LeaderboardEntry() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getBestScore() {
        return bestScore;
    }

    public void setBestScore(long bestScore) {
        this.bestScore = bestScore;
    }

    public String getScoreTimestamp() {
        return scoreTimestamp;
    }

    public void setScoreTimestamp(String scoreTimestamp) {
        this.scoreTimestamp = scoreTimestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
