package com.game.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScoreSubmissionResponse {

    private String playerId;
    private long score;
    @JsonProperty("isNewBest")
    private boolean isNewBest;
    private String periodId;
    private long globalRank;
    private long regionalRank;
    private String region;

    public ScoreSubmissionResponse() {
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public boolean isNewBest() {
        return isNewBest;
    }

    public void setNewBest(boolean newBest) {
        isNewBest = newBest;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public long getGlobalRank() {
        return globalRank;
    }

    public void setGlobalRank(long globalRank) {
        this.globalRank = globalRank;
    }

    public long getRegionalRank() {
        return regionalRank;
    }

    public void setRegionalRank(long regionalRank) {
        this.regionalRank = regionalRank;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
