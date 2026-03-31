package com.game.leaderboard.dto;

public class PlayerProfileResponse {

    private String playerId;
    private String displayName;
    private String region;
    private long bestScore;
    private int totalGamesPlayed;
    private double averageScore;
    private String registeredAt;
    private CurrentWeekRank currentWeekRank;

    public PlayerProfileResponse() {
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public long getBestScore() {
        return bestScore;
    }

    public void setBestScore(long bestScore) {
        this.bestScore = bestScore;
    }

    public int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }

    public void setTotalGamesPlayed(int totalGamesPlayed) {
        this.totalGamesPlayed = totalGamesPlayed;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }

    public CurrentWeekRank getCurrentWeekRank() {
        return currentWeekRank;
    }

    public void setCurrentWeekRank(CurrentWeekRank currentWeekRank) {
        this.currentWeekRank = currentWeekRank;
    }

    public static class CurrentWeekRank {

        private long global;
        private long regional;
        private String periodId;

        public CurrentWeekRank() {
        }

        public CurrentWeekRank(long global, long regional, String periodId) {
            this.global = global;
            this.regional = regional;
            this.periodId = periodId;
        }

        public long getGlobal() {
            return global;
        }

        public void setGlobal(long global) {
            this.global = global;
        }

        public long getRegional() {
            return regional;
        }

        public void setRegional(long regional) {
            this.regional = regional;
        }

        public String getPeriodId() {
            return periodId;
        }

        public void setPeriodId(String periodId) {
            this.periodId = periodId;
        }
    }
}
