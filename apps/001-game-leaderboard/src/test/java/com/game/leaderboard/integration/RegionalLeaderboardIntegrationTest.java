package com.game.leaderboard.integration;

import com.game.leaderboard.TestDataFactory;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.service.PeriodUtil;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegionalLeaderboardIntegrationTest extends AbstractCosmosIntegrationTest {

    private final String currentPeriod = PeriodUtil.getCurrentPeriod();

    @Test
    void getRegionalLeaderboard_returnsOnlyPlayersFromRegion() throws Exception {
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "us-1", "USPlayer1", "US", 9000, "2026-03-28T14:00:00Z"));
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "us-2", "USPlayer2", "US", 8000, "2026-03-28T14:01:00Z"));
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "de-1", "DEPlayer1", "DE", 9500, "2026-03-28T14:02:00Z"));
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "jp-1", "JPPlayer1", "JP", 7000, "2026-03-28T14:03:00Z"));

        mockMvc.perform(get("/api/leaderboards/regions/US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("regional"))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].region").value("US"))
                .andExpect(jsonPath("$.entries[1].region").value("US"))
                .andExpect(jsonPath("$.totalEntries").value(2));
    }

    @Test
    void getRegionalLeaderboard_emptyRegion_returnsEmptyResults() throws Exception {
        leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                currentPeriod, "us-1", "USPlayer1", "US", 9000, "2026-03-28T14:00:00Z"));

        mockMvc.perform(get("/api/leaderboards/regions/JP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(0))
                .andExpect(jsonPath("$.totalEntries").value(0));
    }

    @Test
    void getRegionalLeaderboard_verifyIsolation() throws Exception {
        for (int i = 0; i < 5; i++) {
            leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                    currentPeriod, "us-" + i, "US" + i, "US", 5000 + i * 100, "2026-03-28T14:00:00Z"));
        }
        for (int i = 0; i < 3; i++) {
            leaderboardEntryRepository.save(TestDataFactory.createLeaderboardEntry(
                    currentPeriod, "de-" + i, "DE" + i, "DE", 6000 + i * 100, "2026-03-28T14:00:00Z"));
        }

        mockMvc.perform(get("/api/leaderboards/regions/US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(5));

        mockMvc.perform(get("/api/leaderboards/regions/DE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(3));
    }
}
