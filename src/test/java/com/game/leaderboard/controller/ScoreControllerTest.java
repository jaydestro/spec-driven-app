package com.game.leaderboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.leaderboard.dto.ScoreSubmissionRequest;
import com.game.leaderboard.dto.ScoreSubmissionResponse;
import com.game.leaderboard.exception.GlobalExceptionHandler;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScoreController.class)
class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScoreService scoreService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submitScore_returns201WithValidRequest() throws Exception {
        ScoreSubmissionResponse response = new ScoreSubmissionResponse();
        response.setPlayerId("player-001");
        response.setScore(75000);
        response.setNewBest(true);
        response.setPeriodId("2026-W13");
        response.setGlobalRank(42);
        response.setRegionalRank(12);
        response.setRegion("US");

        when(scoreService.submitScore(any())).thenReturn(response);

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-001", 75000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerId").value("player-001"))
                .andExpect(jsonPath("$.score").value(75000))
                .andExpect(jsonPath("$.isNewBest").value(true))
                .andExpect(jsonPath("$.periodId").value("2026-W13"))
                .andExpect(jsonPath("$.globalRank").value(42))
                .andExpect(jsonPath("$.regionalRank").value(12))
                .andExpect(jsonPath("$.region").value("US"));
    }

    @Test
    void submitScore_returns400ForMissingPlayerId() throws Exception {
        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": 75000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitScore_returns400ForScoreOutOfRange() throws Exception {
        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\": \"player-001\", \"score\": 1000000000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitScore_returns400ForPlayerWithoutRegion() throws Exception {
        when(scoreService.submitScore(any()))
                .thenThrow(new IllegalArgumentException("Player has no region assigned"));

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-001", 100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void submitScore_returns404ForNonExistentPlayer() throws Exception {
        when(scoreService.submitScore(any()))
                .thenThrow(new PlayerNotFoundException("player-unknown"));

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-unknown", 100))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLAYER_NOT_FOUND"));
    }

    @Test
    void submitScore_responseStructure() throws Exception {
        ScoreSubmissionResponse response = new ScoreSubmissionResponse();
        response.setPlayerId("player-001");
        response.setScore(500);
        response.setNewBest(false);
        response.setPeriodId("2026-W13");
        response.setGlobalRank(100);
        response.setRegionalRank(50);
        response.setRegion("DE");

        when(scoreService.submitScore(any())).thenReturn(response);

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScoreSubmissionRequest("player-001", 500))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerId").exists())
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.isNewBest").exists())
                .andExpect(jsonPath("$.periodId").exists())
                .andExpect(jsonPath("$.globalRank").exists())
                .andExpect(jsonPath("$.regionalRank").exists())
                .andExpect(jsonPath("$.region").exists());
    }
}
