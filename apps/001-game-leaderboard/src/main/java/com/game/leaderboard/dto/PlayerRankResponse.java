package com.game.leaderboard.dto;

import java.util.List;

public class PlayerRankResponse {

    private String playerId;
    private String displayName;
    private long bestScore;
    private long rank;
    private String scope;
    private String periodId;
    private List<SurroundingPlayerDto> surroundingPlayers;

    public PlayerRankResponse() {
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

    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public List<SurroundingPlayerDto> getSurroundingPlayers() {
        return surroundingPlayers;
    }

    public void setSurroundingPlayers(List<SurroundingPlayerDto> surroundingPlayers) {
        this.surroundingPlayers = surroundingPlayers;
    }

    public static class SurroundingPlayerDto {

        private long rank;
        private String playerId;
        private String displayName;
        private long bestScore;
        private String region;

        public SurroundingPlayerDto() {
        }

        public long getRank() {
            return rank;
        }

        public void setRank(long rank) {
            this.rank = rank;
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

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
