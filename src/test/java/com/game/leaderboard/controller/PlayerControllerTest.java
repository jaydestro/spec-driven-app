package com.game.leaderboard.controller;

import com.game.leaderboard.dto.PlayerProfileResponse;
import com.game.leaderboard.dto.PlayerProfileResponse.CurrentWeekRank;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    @Test
    void getPlayerProfile_returns200() throws Exception {
        PlayerProfileResponse response = new PlayerProfileResponse();
        response.setPlayerId("p1");
        response.setDisplayName("TestPlayer");
        response.setRegion("US");
        response.setBestScore(5000);
        response.setTotalGamesPlayed(10);
        response.setAverageScore(3000.0);
        response.setRegisteredAt("2026-01-01T00:00:00Z");
        response.setCurrentWeekRank(new CurrentWeekRank(42, 12, "2026-W13"));

        when(playerService.getPlayerProfile(eq("p1"))).thenReturn(response);

        mockMvc.perform(get("/api/players/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("p1"))
                .andExpect(jsonPath("$.displayName").value("TestPlayer"))
                .andExpect(jsonPath("$.region").value("US"))
                .andExpect(jsonPath("$.bestScore").value(5000))
                .andExpect(jsonPath("$.totalGamesPlayed").value(10))
                .andExpect(jsonPath("$.averageScore").value(3000.0))
                .andExpect(jsonPath("$.registeredAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.currentWeekRank.global").value(42))
                .andExpect(jsonPath("$.currentWeekRank.regional").value(12))
                .andExpect(jsonPath("$.currentWeekRank.periodId").value("2026-W13"));
    }

    @Test
    void getPlayerProfile_nonExistentPlayer_returns404() throws Exception {
        when(playerService.getPlayerProfile(eq("unknown")))
                .thenThrow(new PlayerNotFoundException("unknown"));

        mockMvc.perform(get("/api/players/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLAYER_NOT_FOUND"));
    }

    @Test
    void getPlayerProfile_responseStructure() throws Exception {
        PlayerProfileResponse response = new PlayerProfileResponse();
        response.setPlayerId("p1");
        response.setDisplayName("T");
        response.setRegion("US");
        response.setBestScore(0);
        response.setTotalGamesPlayed(0);
        response.setAverageScore(0.0);
        response.setRegisteredAt("2026-01-01T00:00:00Z");
        response.setCurrentWeekRank(new CurrentWeekRank(0, 0, "2026-W13"));

        when(playerService.getPlayerProfile(eq("p1"))).thenReturn(response);

        mockMvc.perform(get("/api/players/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").exists())
                .andExpect(jsonPath("$.displayName").exists())
                .andExpect(jsonPath("$.region").exists())
                .andExpect(jsonPath("$.bestScore").exists())
                .andExpect(jsonPath("$.totalGamesPlayed").exists())
                .andExpect(jsonPath("$.averageScore").exists())
                .andExpect(jsonPath("$.registeredAt").exists())
                .andExpect(jsonPath("$.currentWeekRank").exists())
                .andExpect(jsonPath("$.currentWeekRank.global").exists())
                .andExpect(jsonPath("$.currentWeekRank.regional").exists())
                .andExpect(jsonPath("$.currentWeekRank.periodId").exists());
    }
}
