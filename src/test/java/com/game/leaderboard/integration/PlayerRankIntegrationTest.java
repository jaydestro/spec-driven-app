package com.game.leaderboard.integration;

import com.game.leaderboard.TestDataFactory;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.service.PeriodUtil;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlayerRankIntegrationTest extends AbstractCosmosIntegrationTest {

    private final String currentPeriod = PeriodUtil.getCurrentPeriod();

    @Test
    void getPlayerRank_returnsCorrectRankAndSurroundingPlayers() throws Exception {
        for (int i = 0; i < 20; i++) {
            leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                    currentPeriod, "player-" + String.format("%02d", i),
                    "Player" + i, "US",
                    20000 - i * 1000,
                    "2026-03-28T14:00:00Z"));
        }

        // player-09 has score 11000 → should be rank 10
        mockMvc.perform(get("/api/leaderboards/players/player-09/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("player-09"))
                .andExpect(jsonPath("$.bestScore").value(11000))
                .andExpect(jsonPath("$.rank").value(10))
                .andExpect(jsonPath("$.surroundingPlayers").isArray());
    }

    @Test
    void getPlayerRank_tiedScores_consistentRanking() throws Exception {
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "player-a", "PlayerA", "US", 5000, "2026-03-28T10:00:00Z"));
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "player-b", "PlayerB", "US", 5000, "2026-03-28T12:00:00Z"));
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "player-c", "PlayerC", "DE", 5000, "2026-03-28T14:00:00Z"));

        // All have same score, so all should have rank 1 (no one has a higher score)
        mockMvc.perform(get("/api/leaderboards/players/player-a/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(1));

        mockMvc.perform(get("/api/leaderboards/players/player-b/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(1));
    }

    @Test
    void getPlayerRank_nonExistentPlayer_returns404() throws Exception {
        mockMvc.perform(get("/api/leaderboards/players/nonexistent/rank"))
                .andExpect(status().isNotFound());
    }
}
