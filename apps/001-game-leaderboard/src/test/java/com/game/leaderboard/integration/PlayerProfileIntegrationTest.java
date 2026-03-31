package com.game.leaderboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.leaderboard.TestDataFactory;
import com.game.leaderboard.dto.ScoreSubmissionRequest;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.service.PeriodUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlayerProfileIntegrationTest extends AbstractCosmosIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private final String currentPeriod = PeriodUtil.getCurrentPeriod();

    @Test
    void getPlayerProfile_returnsMatchingFields() throws Exception {
        PlayerProfile player = TestDataFactory.createPlayer("profile-001", "ProfileTest", "US");
        player.setBestScore(5000);
        player.setTotalGamesPlayed(10);
        player.setAverageScore(3000.0);
        player.setTotalScoreSum(30000);
        playerProfileRepository.save(player);

        LeaderboardEntry entry = TestDataFactory.createLeaderboardEntry(
                currentPeriod, "profile-001", "ProfileTest", "US", 5000, "2026-03-28T14:00:00Z");
        leaderboardEntryRepository.save(entry);

        mockMvc.perform(get("/api/players/profile-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("profile-001"))
                .andExpect(jsonPath("$.displayName").value("ProfileTest"))
                .andExpect(jsonPath("$.region").value("US"))
                .andExpect(jsonPath("$.bestScore").value(5000))
                .andExpect(jsonPath("$.totalGamesPlayed").value(10))
                .andExpect(jsonPath("$.averageScore").value(3000.0))
                .andExpect(jsonPath("$.currentWeekRank.global").isNumber())
                .andExpect(jsonPath("$.currentWeekRank.regional").isNumber())
                .andExpect(jsonPath("$.currentWeekRank.periodId").value(currentPeriod));
    }

    @Test
    void getPlayerProfile_afterScoreSubmission_reflectsStats() throws Exception {
        PlayerProfile player = TestDataFactory.createPlayer("profile-002", "ScoreTest", "DE");
        playerProfileRepository.save(player);

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("profile-002", 7500))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/players/profile-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestScore").value(7500))
                .andExpect(jsonPath("$.totalGamesPlayed").value(1))
                .andExpect(jsonPath("$.averageScore").value(7500.0));

        PlayerProfile saved = playerProfileRepository.findById("profile-002").orElseThrow();
        assertThat(saved.getTotalGamesPlayed()).isEqualTo(1);
        assertThat(saved.getBestScore()).isEqualTo(7500);
    }

    @Test
    void getPlayerProfile_unicodeDisplayName_survivesRoundTrip() throws Exception {
        PlayerProfile player = TestDataFactory.createPlayer("profile-003", "游戏玩家🎮", "JP");
        playerProfileRepository.save(player);

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("profile-003", 1000))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/players/profile-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("游戏玩家🎮"));
    }

    @Test
    void getPlayerProfile_nonExistentPlayer_returns404() throws Exception {
        mockMvc.perform(get("/api/players/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
