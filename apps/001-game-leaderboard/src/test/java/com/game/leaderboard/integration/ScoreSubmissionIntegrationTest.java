package com.game.leaderboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.leaderboard.TestDataFactory;
import com.game.leaderboard.dto.ScoreSubmissionRequest;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.model.ScoreEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScoreSubmissionIntegrationTest extends AbstractCosmosIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submitScore_persistsToAllThreeContainers() throws Exception {
        PlayerProfile player = TestDataFactory.createPlayer("player-int001", "IntTestPlayer", "US");
        playerProfileRepository.save(player);

        ScoreSubmissionRequest request = new ScoreSubmissionRequest("player-int001", 85000);

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerId").value("player-int001"))
                .andExpect(jsonPath("$.score").value(85000))
                .andExpect(jsonPath("$.isNewBest").value(true));

        // Verify scores container
        Iterable<ScoreEntry> scores = scoreEntryRepository.findAll();
        List<ScoreEntry> scoreList = new java.util.ArrayList<>();
        scores.forEach(scoreList::add);
        assertThat(scoreList).hasSize(1);
        assertThat(scoreList.get(0).getPlayerId()).isEqualTo("player-int001");
        assertThat(scoreList.get(0).getScore()).isEqualTo(85000);

        // Verify leaderboard-entries container
        Iterable<LeaderboardEntry> entries = leaderboardEntryRepository.findAll();
        List<LeaderboardEntry> entryList = new java.util.ArrayList<>();
        entries.forEach(entryList::add);
        assertThat(entryList).hasSize(1);
        assertThat(entryList.get(0).getPlayerId()).isEqualTo("player-int001");
        assertThat(entryList.get(0).getBestScore()).isEqualTo(85000);

        // Verify players container
        PlayerProfile updated = playerProfileRepository.findById("player-int001").orElseThrow();
        assertThat(updated.getTotalGamesPlayed()).isEqualTo(1);
        assertThat(updated.getBestScore()).isEqualTo(85000);
        assertThat(updated.getTotalScoreSum()).isEqualTo(85000);
        assertThat(updated.getAverageScore()).isEqualTo(85000.0);
    }

    @Test
    void submitScore_higherScoreRetained_statsUpdated() throws Exception {
        PlayerProfile player = TestDataFactory.createPlayer("player-int002", "ScoreKeeper", "DE");
        playerProfileRepository.save(player);

        // Submit first score: 800
        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-int002", 800))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isNewBest").value(true));

        // Submit second score: 300 (lower)
        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-int002", 300))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isNewBest").value(false));

        // Verify leaderboard entry still shows 800
        Iterable<LeaderboardEntry> entries = leaderboardEntryRepository.findAll();
        List<LeaderboardEntry> entryList = new java.util.ArrayList<>();
        entries.forEach(entryList::add);
        LeaderboardEntry entry = entryList.stream()
                .filter(e -> e.getPlayerId().equals("player-int002"))
                .findFirst().orElseThrow();
        assertThat(entry.getBestScore()).isEqualTo(800);

        // Verify player stats
        PlayerProfile updated = playerProfileRepository.findById("player-int002").orElseThrow();
        assertThat(updated.getTotalGamesPlayed()).isEqualTo(2);
        assertThat(updated.getAverageScore()).isCloseTo(550.0, org.assertj.core.data.Offset.offset(0.01));
        assertThat(updated.getBestScore()).isEqualTo(800);
    }

    @Test
    void submitScore_playerWithoutRegion_returns400_noDocumentsWritten() throws Exception {
        PlayerProfile player = TestDataFactory.createPlayer("player-int003", "NoRegion", null);
        player.setRegion(null);
        playerProfileRepository.save(player);

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-int003", 500))))
                .andExpect(status().isBadRequest());

        // Verify no score entries written
        Iterable<ScoreEntry> scores = scoreEntryRepository.findAll();
        List<ScoreEntry> scoreList = new java.util.ArrayList<>();
        scores.forEach(scoreList::add);
        assertThat(scoreList).isEmpty();

        // Verify no leaderboard entries written
        Iterable<LeaderboardEntry> entries = leaderboardEntryRepository.findAll();
        List<LeaderboardEntry> entryList = new java.util.ArrayList<>();
        entries.forEach(entryList::add);
        assertThat(entryList).isEmpty();
    }
}
