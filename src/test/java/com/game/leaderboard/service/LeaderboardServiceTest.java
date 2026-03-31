package com.game.leaderboard.service;

import com.game.leaderboard.dto.LeaderboardResponse;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.repository.LeaderboardEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @Test
    void getGlobalLeaderboard_happyPath_returnsSortedEntries() {
        LeaderboardEntry e1 = createEntry("p1", 1000, "US");
        LeaderboardEntry e2 = createEntry("p2", 800, "DE");

        when(leaderboardEntryRepository.findTopByPeriod(anyString(), eq(0), eq(50)))
                .thenReturn(List.of(e1, e2));
        when(leaderboardEntryRepository.countByPeriod(anyString())).thenReturn(2L);

        LeaderboardResponse response = leaderboardService.getGlobalLeaderboard(null, 1, 50);

        assertThat(response.getScope()).isEqualTo("global");
        assertThat(response.getEntries()).hasSize(2);
        assertThat(response.getEntries().get(0).getBestScore()).isEqualTo(1000);
        assertThat(response.getEntries().get(1).getBestScore()).isEqualTo(800);
        assertThat(response.getTotalEntries()).isEqualTo(2);
    }

    @Test
    void getGlobalLeaderboard_defaultPeriodId_usesCurrentWeek() {
        when(leaderboardEntryRepository.findTopByPeriod(anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(leaderboardEntryRepository.countByPeriod(anyString())).thenReturn(0L);

        LeaderboardResponse response = leaderboardService.getGlobalLeaderboard(null, 1, 50);

        assertThat(response.getPeriodId()).isEqualTo(PeriodUtil.getCurrentPeriod());
    }

    @Test
    void getGlobalLeaderboard_pageZero_throws() {
        assertThatThrownBy(() -> leaderboardService.getGlobalLeaderboard(null, 0, 50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getGlobalLeaderboard_pageSize101_throws() {
        assertThatThrownBy(() -> leaderboardService.getGlobalLeaderboard(null, 1, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getGlobalLeaderboard_emptyLeaderboard_returnsEmptyEntries() {
        when(leaderboardEntryRepository.findTopByPeriod(anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(leaderboardEntryRepository.countByPeriod(anyString())).thenReturn(0L);

        LeaderboardResponse response = leaderboardService.getGlobalLeaderboard("2026-W01", 1, 50);

        assertThat(response.getEntries()).isEmpty();
        assertThat(response.getTotalEntries()).isEqualTo(0);
    }

    @Test
    void getGlobalLeaderboard_rankNumbering_isCorrect() {
        LeaderboardEntry e1 = createEntry("p1", 1000, "US");
        LeaderboardEntry e2 = createEntry("p2", 800, "DE");
        LeaderboardEntry e3 = createEntry("p3", 600, "JP");

        when(leaderboardEntryRepository.findTopByPeriod(anyString(), eq(0), eq(50)))
                .thenReturn(List.of(e1, e2, e3));
        when(leaderboardEntryRepository.countByPeriod(anyString())).thenReturn(3L);

        LeaderboardResponse response = leaderboardService.getGlobalLeaderboard("2026-W13", 1, 50);

        assertThat(response.getEntries().get(0).getRank()).isEqualTo(1);
        assertThat(response.getEntries().get(1).getRank()).isEqualTo(2);
        assertThat(response.getEntries().get(2).getRank()).isEqualTo(3);
    }

    @Test
    void getRegionalLeaderboard_returnsOnlyMatchingRegion() {
        LeaderboardEntry usEntry = createEntry("p1", 1000, "US");

        when(leaderboardEntryRepository.findTopByPeriodAndRegion(anyString(), eq("US"), eq(0), eq(50)))
                .thenReturn(List.of(usEntry));
        when(leaderboardEntryRepository.countByPeriodAndRegion(anyString(), eq("US"))).thenReturn(1L);

        LeaderboardResponse response = leaderboardService.getRegionalLeaderboard("US", null, 1, 50);

        assertThat(response.getScope()).isEqualTo("regional");
        assertThat(response.getEntries()).hasSize(1);
        assertThat(response.getEntries().get(0).getRegion()).isEqualTo("US");
    }

    @Test
    void getRegionalLeaderboard_invalidRegion_throws() {
        assertThatThrownBy(() -> leaderboardService.getRegionalLeaderboard("XX1", null, 1, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    private LeaderboardEntry createEntry(String playerId, long score, String region) {
        LeaderboardEntry e = new LeaderboardEntry();
        e.setId("period_" + playerId);
        e.setPeriodId("2026-W13");
        e.setPlayerId(playerId);
        e.setDisplayName("Player " + playerId);
        e.setBestScore(score);
        e.setRegion(region);
        e.setScoreTimestamp("2026-03-28T14:30:00Z");
        return e;
    }
}
