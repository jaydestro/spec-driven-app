package com.game.leaderboard.service;

import com.game.leaderboard.dto.PlayerProfileResponse;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.repository.PlayerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void getPlayerProfile_happyPath_returnsProfileWithRanks() {
        PlayerProfile player = new PlayerProfile();
        player.setId("p1");
        player.setPlayerId("p1");
        player.setDisplayName("TestPlayer");
        player.setRegion("US");
        player.setBestScore(5000);
        player.setTotalGamesPlayed(10);
        player.setAverageScore(3000.0);
        player.setRegisteredAt("2026-01-01T00:00:00Z");

        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setBestScore(5000);

        when(playerProfileRepository.findById("p1")).thenReturn(Optional.of(player));
        when(leaderboardService.findPlayerEntry(anyString(), eq("p1"))).thenReturn(entry);
        when(leaderboardService.getGlobalRank(anyString(), eq(5000L))).thenReturn(42L);
        when(leaderboardService.getRegionalRank(anyString(), eq("US"), eq(5000L))).thenReturn(12L);

        PlayerProfileResponse response = playerService.getPlayerProfile("p1");

        assertThat(response.getPlayerId()).isEqualTo("p1");
        assertThat(response.getDisplayName()).isEqualTo("TestPlayer");
        assertThat(response.getRegion()).isEqualTo("US");
        assertThat(response.getBestScore()).isEqualTo(5000);
        assertThat(response.getTotalGamesPlayed()).isEqualTo(10);
        assertThat(response.getAverageScore()).isEqualTo(3000.0);
        assertThat(response.getCurrentWeekRank().getGlobal()).isEqualTo(42);
        assertThat(response.getCurrentWeekRank().getRegional()).isEqualTo(12);
    }

    @Test
    void getPlayerProfile_nonExistentPlayer_throws() {
        when(playerProfileRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getPlayerProfile("unknown"))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void getPlayerProfile_fieldsMappedCorrectly() {
        PlayerProfile player = new PlayerProfile();
        player.setId("p2");
        player.setPlayerId("p2");
        player.setDisplayName("MapTest");
        player.setRegion("DE");
        player.setBestScore(9999);
        player.setTotalGamesPlayed(42);
        player.setAverageScore(7777.5);
        player.setRegisteredAt("2026-02-15T10:00:00Z");

        when(playerProfileRepository.findById("p2")).thenReturn(Optional.of(player));
        when(leaderboardService.findPlayerEntry(anyString(), eq("p2"))).thenReturn(null);

        PlayerProfileResponse response = playerService.getPlayerProfile("p2");

        assertThat(response.getDisplayName()).isEqualTo("MapTest");
        assertThat(response.getRegion()).isEqualTo("DE");
        assertThat(response.getBestScore()).isEqualTo(9999);
        assertThat(response.getTotalGamesPlayed()).isEqualTo(42);
        assertThat(response.getAverageScore()).isEqualTo(7777.5);
        assertThat(response.getRegisteredAt()).isEqualTo("2026-02-15T10:00:00Z");
        assertThat(response.getCurrentWeekRank().getGlobal()).isEqualTo(0);
    }
}
