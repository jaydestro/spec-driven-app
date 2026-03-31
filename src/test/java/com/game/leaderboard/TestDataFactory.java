package com.game.leaderboard;

import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.model.ScoreEntry;

import java.time.Instant;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static PlayerProfile createPlayer(String playerId, String displayName, String region) {
        PlayerProfile p = new PlayerProfile();
        p.setId(playerId);
        p.setPlayerId(playerId);
        p.setDisplayName(displayName);
        p.setRegion(region);
        p.setBestScore(0);
        p.setTotalGamesPlayed(0);
        p.setAverageScore(0.0);
        p.setTotalScoreSum(0);
        p.setRegisteredAt(Instant.now().toString());
        p.setLastUpdated(Instant.now().toString());
        return p;
    }

    public static LeaderboardEntry createLeaderboardEntry(String periodId, String playerId,
                                                           String displayName, String region,
                                                           long bestScore, String scoreTimestamp) {
        LeaderboardEntry e = new LeaderboardEntry();
        e.setId(periodId + "_" + playerId);
        e.setPeriodId(periodId);
        e.setPlayerId(playerId);
        e.setDisplayName(displayName);
        e.setRegion(region);
        e.setBestScore(bestScore);
        e.setScoreTimestamp(scoreTimestamp);
        return e;
    }

    public static ScoreEntry createScoreEntry(String playerId, long score,
                                               String periodId, String region) {
        ScoreEntry s = new ScoreEntry();
        s.setPlayerId(playerId);
        s.setScore(score);
        s.setPeriodId(periodId);
        s.setRegion(region);
        s.setSubmittedAt(Instant.now().toString());
        return s;
    }
}
