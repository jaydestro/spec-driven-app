package com.game.leaderboard.service;

import com.game.leaderboard.dto.ScoreSubmissionRequest;
import com.game.leaderboard.dto.ScoreSubmissionResponse;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.model.PlayerProfile;
import com.game.leaderboard.model.ScoreEntry;
import com.game.leaderboard.repository.LeaderboardEntryRepository;
import com.game.leaderboard.repository.PlayerProfileRepository;
import com.game.leaderboard.repository.ScoreEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ScoreService {

    private final ScoreEntryRepository scoreEntryRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public ScoreService(ScoreEntryRepository scoreEntryRepository,
                        LeaderboardEntryRepository leaderboardEntryRepository,
                        PlayerProfileRepository playerProfileRepository) {
        this.scoreEntryRepository = scoreEntryRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    public ScoreSubmissionResponse submitScore(ScoreSubmissionRequest request) {
        if (request.getScore() < 0 || request.getScore() > 999_999_999) {
            throw new IllegalArgumentException("Score must be between 0 and 999,999,999");
        }

        PlayerProfile player = playerProfileRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new PlayerNotFoundException(request.getPlayerId()));

        if (player.getRegion() == null || player.getRegion().isBlank()) {
            throw new IllegalArgumentException("Player has no region assigned");
        }

        String periodId = PeriodUtil.getCurrentPeriod();
        String now = Instant.now().toString();

        ScoreEntry scoreEntry = new ScoreEntry();
        scoreEntry.setPlayerId(request.getPlayerId());
        scoreEntry.setScore(request.getScore());
        scoreEntry.setSubmittedAt(now);
        scoreEntry.setPeriodId(periodId);
        scoreEntry.setRegion(player.getRegion());
        scoreEntryRepository.save(scoreEntry);

        boolean isNewBest = false;
        long currentBestScore;

        List<LeaderboardEntry> existing = leaderboardEntryRepository
                .findByPeriodAndPlayer(periodId, request.getPlayerId());

        if (existing.isEmpty()) {
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setId(periodId + "_" + request.getPlayerId());
            entry.setPeriodId(periodId);
            entry.setRegion(player.getRegion());
            entry.setPlayerId(request.getPlayerId());
            entry.setDisplayName(player.getDisplayName());
            entry.setBestScore(request.getScore());
            entry.setScoreTimestamp(now);
            leaderboardEntryRepository.save(entry);
            isNewBest = true;
            currentBestScore = request.getScore();
        } else {
            LeaderboardEntry entry = existing.get(0);
            if (request.getScore() > entry.getBestScore()) {
                entry.setBestScore(request.getScore());
                entry.setScoreTimestamp(now);
                leaderboardEntryRepository.save(entry);
                isNewBest = true;
                currentBestScore = request.getScore();
            } else {
                currentBestScore = entry.getBestScore();
            }
        }

        player.setTotalGamesPlayed(player.getTotalGamesPlayed() + 1);
        player.setTotalScoreSum(player.getTotalScoreSum() + request.getScore());
        player.setAverageScore((double) player.getTotalScoreSum() / player.getTotalGamesPlayed());
        if (request.getScore() > player.getBestScore()) {
            player.setBestScore(request.getScore());
        }
        player.setLastUpdated(now);
        playerProfileRepository.save(player);

        long globalRank = leaderboardEntryRepository.countWithHigherScore(periodId, currentBestScore) + 1;
        long regionalRank = leaderboardEntryRepository.countWithHigherScoreInRegion(
                periodId, player.getRegion(), currentBestScore) + 1;

        ScoreSubmissionResponse response = new ScoreSubmissionResponse();
        response.setPlayerId(request.getPlayerId());
        response.setScore(request.getScore());
        response.setNewBest(isNewBest);
        response.setPeriodId(periodId);
        response.setGlobalRank(globalRank);
        response.setRegionalRank(regionalRank);
        response.setRegion(player.getRegion());
        return response;
    }
}
