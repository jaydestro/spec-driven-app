package com.game.leaderboard.service;

import com.game.leaderboard.dto.PlayerProfileResponse;
import com.game.leaderboard.dto.PlayerProfileResponse.CurrentWeekRank;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.repository.PlayerProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final PlayerProfileRepository playerProfileRepository;
    private final LeaderboardService leaderboardService;

    public PlayerService(PlayerProfileRepository playerProfileRepository,
                         LeaderboardService leaderboardService) {
        this.playerProfileRepository = playerProfileRepository;
        this.leaderboardService = leaderboardService;
    }

    public PlayerProfileResponse getPlayerProfile(String playerId) {
        PlayerProfile player = playerProfileRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));

        String currentPeriod = PeriodUtil.getCurrentPeriod();
        long globalRank = 0;
        long regionalRank = 0;

        LeaderboardEntry entry = leaderboardService.findPlayerEntry(currentPeriod, playerId);
        if (entry != null) {
            globalRank = leaderboardService.getGlobalRank(currentPeriod, entry.getBestScore());
            if (player.getRegion() != null) {
                regionalRank = leaderboardService.getRegionalRank(
                        currentPeriod, player.getRegion(), entry.getBestScore());
            }
        }

        PlayerProfileResponse response = new PlayerProfileResponse();
        response.setPlayerId(player.getPlayerId());
        response.setDisplayName(player.getDisplayName());
        response.setRegion(player.getRegion());
        response.setBestScore(player.getBestScore());
        response.setTotalGamesPlayed(player.getTotalGamesPlayed());
        response.setAverageScore(player.getAverageScore());
        response.setRegisteredAt(player.getRegisteredAt());
        response.setCurrentWeekRank(new CurrentWeekRank(globalRank, regionalRank, currentPeriod));

        return response;
    }
}
