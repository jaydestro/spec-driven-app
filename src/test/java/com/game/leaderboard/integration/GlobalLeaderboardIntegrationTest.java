package com.game.leaderboard.integration;

import com.game.leaderboard.TestDataFactory;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.service.PeriodUtil;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalLeaderboardIntegrationTest extends AbstractCosmosIntegrationTest {

    private final String currentPeriod = PeriodUtil.getCurrentPeriod();

    @Test
    void getGlobalLeaderboard_returnsSortedByScoreDescending() throws Exception {
        for (int i = 0; i < 10; i++) {
            LeaderboardEntry entry = TestDataFactory.createLeaderboardEntry(
                    currentPeriod, "player-" + i, "Player" + i,
                    i % 2 == 0 ? "US" : "DE",
                    (10 - i) * 1000,
                    "2026-03-28T14:" + String.format("%02d", i) + ":00Z");
            leaderboardEntryRepository.save(entry);
        }

        mockMvc.perform(get("/api/leaderboards/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(10))
                .andExpect(jsonPath("$.entries[0].bestScore").value(10000))
                .andExpect(jsonPath("$.entries[9].bestScore").value(1000))
                .andExpect(jsonPath("$.totalEntries").value(10));
    }

    @Test
    void getGlobalLeaderboard_tiedScores_earlierTimestampRanksHigher() throws Exception {
        LeaderboardEntry earlier = TestDataFactory.createLeaderboardEntry(
                currentPeriod, "player-early", "EarlyPlayer", "US",
                500, "2026-03-28T10:00:00Z");
        LeaderboardEntry later = TestDataFactory.createLeaderboardEntry(
                currentPeriod, "player-late", "LatePlayer", "DE",
                500, "2026-03-28T14:00:00Z");

        leaderboardEntryRepository.save(earlier);
        leaderboardEntryRepository.save(later);

        mockMvc.perform(get("/api/leaderboards/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].playerId").value("player-early"))
                .andExpect(jsonPath("$.entries[1].playerId").value("player-late"));
    }

    @Test
    void getGlobalLeaderboard_pagination_page1() throws Exception {
        for (int i = 0; i < 75; i++) {
            LeaderboardEntry entry = TestDataFactory.createLeaderboardEntry(
                    currentPeriod, "player-" + String.format("%03d", i), "P" + i,
                    "US", 75000 - i,
                    "2026-03-28T14:00:00Z");
            leaderboardEntryRepository.save(entry);
        }

        mockMvc.perform(get("/api/leaderboards/global")
                        .param("page", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(50))
                .andExpect(jsonPath("$.totalEntries").value(75))
                .andExpect(jsonPath("$.entries[0].rank").value(1));
    }

    @Test
    void getGlobalLeaderboard_pagination_page2() throws Exception {
        for (int i = 0; i < 75; i++) {
            LeaderboardEntry entry = TestDataFactory.createLeaderboardEntry(
                    currentPeriod, "player-" + String.format("%03d", i), "P" + i,
                    "US", 75000 - i,
                    "2026-03-28T14:00:00Z");
            leaderboardEntryRepository.save(entry);
        }

        mockMvc.perform(get("/api/leaderboards/global")
                        .param("page", "2")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(25))
                .andExpect(jsonPath("$.totalEntries").value(75))
                .andExpect(jsonPath("$.entries[0].rank").value(51));
    }
}
