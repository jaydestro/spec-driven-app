package com.game.leaderboard.service;

import com.game.leaderboard.dto.ScoreSubmissionRequest;
import com.game.leaderboard.dto.ScoreSubmissionResponse;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.repository.LeaderboardEntryRepository;
import com.game.leaderboard.repository.PlayerProfileRepository;
import com.game.leaderboard.repository.ScoreEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreEntryRepository scoreEntryRepository;

    @Mock
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @InjectMocks
    private ScoreService scoreService;

    private PlayerProfile testPlayer;

    @BeforeEach
    void setUp() {
        testPlayer = new PlayerProfile();
        testPlayer.setId("player-001");
        testPlayer.setPlayerId("player-001");
        testPlayer.setDisplayName("TestPlayer");
        testPlayer.setRegion("US");
        testPlayer.setBestScore(0);
        testPlayer.setTotalGamesPlayed(0);
        testPlayer.setTotalScoreSum(0);
        testPlayer.setAverageScore(0.0);
    }

    @Test
    void submitScore_happyPath_createsEntryAndReturnsRank() {
        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));
        when(leaderboardEntryRepository.findByPeriodAndPlayer(anyString(), eq("player-001")))
                .thenReturn(Collections.emptyList());
        when(leaderboardEntryRepository.countWithHigherScore(anyString(), eq(75000L))).thenReturn(5L);
        when(leaderboardEntryRepository.countWithHigherScoreInRegion(anyString(), eq("US"), eq(75000L))).thenReturn(2L);

        ScoreSubmissionResponse response = scoreService.submitScore(new ScoreSubmissionRequest("player-001", 75000));

        assertThat(response.getPlayerId()).isEqualTo("player-001");
        assertThat(response.getScore()).isEqualTo(75000);
        assertThat(response.isNewBest()).isTrue();
        assertThat(response.getGlobalRank()).isEqualTo(6);
        assertThat(response.getRegionalRank()).isEqualTo(3);
        assertThat(response.getRegion()).isEqualTo("US");
        assertThat(response.getPeriodId()).isNotNull();

        verify(scoreEntryRepository).save(any());
        verify(leaderboardEntryRepository).save(any());
        verify(playerProfileRepository).save(any());
    }

    @Test
    void submitScore_higherScoreRetained() {
        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));

        LeaderboardEntry existingEntry = new LeaderboardEntry();
        existingEntry.setId("period_player-001");
        existingEntry.setBestScore(800);
        existingEntry.setPeriodId(PeriodUtil.getCurrentPeriod());
        existingEntry.setPlayerId("player-001");
        existingEntry.setRegion("US");

        when(leaderboardEntryRepository.findByPeriodAndPlayer(anyString(), eq("player-001")))
                .thenReturn(List.of(existingEntry));
        when(leaderboardEntryRepository.countWithHigherScore(anyString(), eq(800L))).thenReturn(0L);
        when(leaderboardEntryRepository.countWithHigherScoreInRegion(anyString(), eq("US"), eq(800L))).thenReturn(0L);

        ScoreSubmissionResponse response = scoreService.submitScore(new ScoreSubmissionRequest("player-001", 300));

        assertThat(response.isNewBest()).isFalse();
        assertThat(response.getGlobalRank()).isEqualTo(1);
    }

    @Test
    void submitScore_lowerScore_doesNotOverwriteLeaderboardEntry() {
        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));

        LeaderboardEntry existingEntry = new LeaderboardEntry();
        existingEntry.setId("period_player-001");
        existingEntry.setBestScore(800);
        existingEntry.setPeriodId(PeriodUtil.getCurrentPeriod());
        existingEntry.setPlayerId("player-001");
        existingEntry.setRegion("US");

        when(leaderboardEntryRepository.findByPeriodAndPlayer(anyString(), eq("player-001")))
                .thenReturn(List.of(existingEntry));
        when(leaderboardEntryRepository.countWithHigherScore(anyString(), anyLong())).thenReturn(0L);
        when(leaderboardEntryRepository.countWithHigherScoreInRegion(anyString(), anyString(), anyLong())).thenReturn(0L);

        scoreService.submitScore(new ScoreSubmissionRequest("player-001", 300));

        ArgumentCaptor<LeaderboardEntry> captor = ArgumentCaptor.forClass(LeaderboardEntry.class);
        verify(leaderboardEntryRepository, never()).save(captor.capture());
    }

    @Test
    void submitScore_scoreBoundary_zeroAccepted() {
        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));
        when(leaderboardEntryRepository.findByPeriodAndPlayer(anyString(), eq("player-001")))
                .thenReturn(Collections.emptyList());
        when(leaderboardEntryRepository.countWithHigherScore(anyString(), anyLong())).thenReturn(0L);
        when(leaderboardEntryRepository.countWithHigherScoreInRegion(anyString(), anyString(), anyLong())).thenReturn(0L);

        ScoreSubmissionResponse response = scoreService.submitScore(new ScoreSubmissionRequest("player-001", 0));

        assertThat(response.getScore()).isEqualTo(0);
    }

    @Test
    void submitScore_scoreBoundary_maxAccepted() {
        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));
        when(leaderboardEntryRepository.findByPeriodAndPlayer(anyString(), eq("player-001")))
                .thenReturn(Collections.emptyList());
        when(leaderboardEntryRepository.countWithHigherScore(anyString(), anyLong())).thenReturn(0L);
        when(leaderboardEntryRepository.countWithHigherScoreInRegion(anyString(), anyString(), anyLong())).thenReturn(0L);

        ScoreSubmissionResponse response = scoreService.submitScore(new ScoreSubmissionRequest("player-001", 999_999_999));

        assertThat(response.getScore()).isEqualTo(999_999_999);
    }

    @Test
    void submitScore_negativeScore_throws() {
        assertThatThrownBy(() -> scoreService.submitScore(new ScoreSubmissionRequest("player-001", -1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitScore_missingPlayer_throws() {
        when(playerProfileRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoreService.submitScore(new ScoreSubmissionRequest("unknown", 100)))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void submitScore_playerWithoutRegion_throws() {
        testPlayer.setRegion(null);
        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));

        assertThatThrownBy(() -> scoreService.submitScore(new ScoreSubmissionRequest("player-001", 100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");
    }

    @Test
    void submitScore_averageScoreRecalculation() {
        testPlayer.setTotalGamesPlayed(2);
        testPlayer.setTotalScoreSum(1000);
        testPlayer.setAverageScore(500.0);
        testPlayer.setBestScore(600);

        when(playerProfileRepository.findById("player-001")).thenReturn(Optional.of(testPlayer));

        LeaderboardEntry existingEntry = new LeaderboardEntry();
        existingEntry.setId("period_player-001");
        existingEntry.setBestScore(600);
        existingEntry.setPeriodId(PeriodUtil.getCurrentPeriod());
        existingEntry.setPlayerId("player-001");
        existingEntry.setRegion("US");

        when(leaderboardEntryRepository.findByPeriodAndPlayer(anyString(), eq("player-001")))
                .thenReturn(List.of(existingEntry));
        when(leaderboardEntryRepository.countWithHigherScore(anyString(), anyLong())).thenReturn(0L);
        when(leaderboardEntryRepository.countWithHigherScoreInRegion(anyString(), anyString(), anyLong())).thenReturn(0L);

        scoreService.submitScore(new ScoreSubmissionRequest("player-001", 400));

        ArgumentCaptor<PlayerProfile> captor = ArgumentCaptor.forClass(PlayerProfile.class);
        verify(playerProfileRepository).save(captor.capture());
        PlayerProfile saved = captor.getValue();

        assertThat(saved.getTotalGamesPlayed()).isEqualTo(3);
        assertThat(saved.getTotalScoreSum()).isEqualTo(1400);
        assertThat(saved.getAverageScore()).isCloseTo(1400.0 / 3, org.assertj.core.data.Offset.offset(0.01));
        assertThat(saved.getBestScore()).isEqualTo(600); // not overwritten
    }
}
