package com.game.leaderboard.dto;

import java.util.List;

public class LeaderboardResponse {

    private String periodId;
    private String scope;
    private int page;
    private int pageSize;
    private long totalEntries;
    private List<LeaderboardEntryDto> entries;

    public LeaderboardResponse() {
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public List<LeaderboardEntryDto> getEntries() {
        return entries;
    }

    public void setEntries(List<LeaderboardEntryDto> entries) {
        this.entries = entries;
    }

    public static class LeaderboardEntryDto {

        private long rank;
        private String playerId;
        private String displayName;
        private long bestScore;
        private String region;
        private String scoreTimestamp;

        public LeaderboardEntryDto() {
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

        public String getScoreTimestamp() {
            return scoreTimestamp;
        }

        public void setScoreTimestamp(String scoreTimestamp) {
            this.scoreTimestamp = scoreTimestamp;
        }
    }
}
