package com.game.leaderboard.controller;

import com.game.leaderboard.dto.LeaderboardResponse;
import com.game.leaderboard.dto.LeaderboardResponse.LeaderboardEntryDto;
import com.game.leaderboard.dto.PlayerRankResponse;
import com.game.leaderboard.dto.PlayerRankResponse.SurroundingPlayerDto;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaderboardController.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @Test
    void getGlobalLeaderboard_returns200() throws Exception {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setPeriodId("2026-W13");
        response.setScope("global");
        response.setPage(1);
        response.setPageSize(50);
        response.setTotalEntries(0);
        response.setEntries(List.of());

        when(leaderboardService.getGlobalLeaderboard(any(), eq(1), eq(50))).thenReturn(response);

        mockMvc.perform(get("/api/leaderboards/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodId").value("2026-W13"))
                .andExpect(jsonPath("$.scope").value("global"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(50));
    }

    @Test
    void getGlobalLeaderboard_defaultPagination() throws Exception {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setPeriodId("2026-W13");
        response.setScope("global");
        response.setPage(1);
        response.setPageSize(50);
        response.setTotalEntries(0);
        response.setEntries(List.of());

        when(leaderboardService.getGlobalLeaderboard(any(), eq(1), eq(50))).thenReturn(response);

        mockMvc.perform(get("/api/leaderboards/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(50));
    }

    @Test
    void getGlobalLeaderboard_invalidPage_returns400() throws Exception {
        mockMvc.perform(get("/api/leaderboards/global").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGlobalLeaderboard_invalidPageSize_returns400() throws Exception {
        mockMvc.perform(get("/api/leaderboards/global").param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGlobalLeaderboard_responseStructure() throws Exception {
        LeaderboardEntryDto entry = new LeaderboardEntryDto();
        entry.setRank(1);
        entry.setPlayerId("p1");
        entry.setDisplayName("TestPlayer");
        entry.setBestScore(1000);
        entry.setRegion("US");
        entry.setScoreTimestamp("2026-03-28T14:30:00Z");

        LeaderboardResponse response = new LeaderboardResponse();
        response.setPeriodId("2026-W13");
        response.setScope("global");
        response.setPage(1);
        response.setPageSize(50);
        response.setTotalEntries(1);
        response.setEntries(List.of(entry));

        when(leaderboardService.getGlobalLeaderboard(any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/leaderboards/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodId").exists())
                .andExpect(jsonPath("$.scope").exists())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.pageSize").exists())
                .andExpect(jsonPath("$.totalEntries").exists())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].playerId").value("p1"))
                .andExpect(jsonPath("$.entries[0].displayName").value("TestPlayer"))
                .andExpect(jsonPath("$.entries[0].bestScore").value(1000))
                .andExpect(jsonPath("$.entries[0].region").value("US"));
    }

    @Test
    void getRegionalLeaderboard_returns200() throws Exception {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setPeriodId("2026-W13");
        response.setScope("regional");
        response.setPage(1);
        response.setPageSize(50);
        response.setTotalEntries(0);
        response.setEntries(List.of());

        when(leaderboardService.getRegionalLeaderboard(eq("US"), any(), eq(1), eq(50))).thenReturn(response);

        mockMvc.perform(get("/api/leaderboards/regions/US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("regional"));
    }

    @Test
    void getRegionalLeaderboard_invalidRegion_returns400() throws Exception {
        when(leaderboardService.getRegionalLeaderboard(eq("XX1"), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Region must be a valid ISO 3166-1 alpha-2 code"));

        mockMvc.perform(get("/api/leaderboards/regions/XX1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPlayerRank_returns200() throws Exception {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setPlayerId("p1");
        entry.setDisplayName("TestPlayer");
        entry.setBestScore(1000);
        entry.setRegion("US");

        when(leaderboardService.findPlayerEntry(anyString(), eq("p1"))).thenReturn(entry);
        when(leaderboardService.getGlobalRank(anyString(), eq(1000L))).thenReturn(5L);
        when(leaderboardService.getSurroundingPlayers(anyString(), eq(1000L), eq(10)))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/leaderboards/players/p1/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("p1"))
                .andExpect(jsonPath("$.rank").value(5))
                .andExpect(jsonPath("$.surroundingPlayers").isArray());
    }

    @Test
    void getPlayerRank_nonExistentPlayer_returns404() throws Exception {
        when(leaderboardService.findPlayerEntry(anyString(), eq("unknown"))).thenReturn(null);

        mockMvc.perform(get("/api/leaderboards/players/unknown/rank"))
                .andExpect(status().isNotFound());
    }
}
