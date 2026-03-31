package com.game.leaderboard.service;

import com.game.leaderboard.dto.LeaderboardResponse;
import com.game.leaderboard.dto.LeaderboardResponse.LeaderboardEntryDto;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.repository.LeaderboardEntryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;

    public LeaderboardService(LeaderboardEntryRepository leaderboardEntryRepository) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
    }

    public LeaderboardResponse getGlobalLeaderboard(String periodId, int page, int pageSize) {
        if (periodId == null || periodId.isBlank()) {
            periodId = PeriodUtil.getCurrentPeriod();
        }
        validatePagination(page, pageSize);

        int offset = (page - 1) * pageSize;
        List<LeaderboardEntry> entries = leaderboardEntryRepository.findTopByPeriod(periodId, offset, pageSize);
        long totalEntries = leaderboardEntryRepository.countByPeriod(periodId);

        return buildResponse(periodId, "global", page, pageSize, totalEntries, entries, offset);
    }

    public LeaderboardResponse getRegionalLeaderboard(String region, String periodId, int page, int pageSize) {
        validateRegion(region);
        if (periodId == null || periodId.isBlank()) {
            periodId = PeriodUtil.getCurrentPeriod();
        }
        validatePagination(page, pageSize);

        int offset = (page - 1) * pageSize;
        List<LeaderboardEntry> entries = leaderboardEntryRepository
                .findTopByPeriodAndRegion(periodId, region, offset, pageSize);
        long totalEntries = leaderboardEntryRepository.countByPeriodAndRegion(periodId, region);

        return buildResponse(periodId, "regional", page, pageSize, totalEntries, entries, offset);
    }

    public long getGlobalRank(String periodId, long bestScore) {
        return leaderboardEntryRepository.countWithHigherScore(periodId, bestScore) + 1;
    }

    public long getRegionalRank(String periodId, String region, long bestScore) {
        return leaderboardEntryRepository.countWithHigherScoreInRegion(periodId, region, bestScore) + 1;
    }

    public LeaderboardEntry findPlayerEntry(String periodId, String playerId) {
        List<LeaderboardEntry> entries = leaderboardEntryRepository.findByPeriodAndPlayer(periodId, playerId);
        return entries.isEmpty() ? null : entries.get(0);
    }

    public List<LeaderboardEntry> getSurroundingPlayers(String periodId, long playerScore, int count) {
        List<LeaderboardEntry> above = leaderboardEntryRepository.findSurroundingAbove(periodId, playerScore, count);
        List<LeaderboardEntry> below = leaderboardEntryRepository.findSurroundingBelow(periodId, playerScore, count);

        List<LeaderboardEntry> result = new ArrayList<>(above);
        for (LeaderboardEntry e : below) {
            if (result.stream().noneMatch(r -> r.getPlayerId().equals(e.getPlayerId()))) {
                result.add(e);
            }
        }
        result.sort((a, b) -> {
            int cmp = Long.compare(b.getBestScore(), a.getBestScore());
            if (cmp != 0) return cmp;
            return a.getScoreTimestamp().compareTo(b.getScoreTimestamp());
        });

        return result;
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be >= 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("PageSize must be between 1 and 100");
        }
    }

    private void validateRegion(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region is required");
        }
        if (!region.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("Region must be a valid ISO 3166-1 alpha-2 code (e.g., US, DE, JP)");
        }
    }

    private LeaderboardResponse buildResponse(String periodId, String scope, int page, int pageSize,
                                               long totalEntries, List<LeaderboardEntry> entries, int offset) {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setPeriodId(periodId);
        response.setScope(scope);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalEntries(totalEntries);
        response.setEntries(mapToEntryDtos(entries, offset));
        return response;
    }

    private List<LeaderboardEntryDto> mapToEntryDtos(List<LeaderboardEntry> entries, int offset) {
        List<LeaderboardEntryDto> dtos = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry e = entries.get(i);
            LeaderboardEntryDto dto = new LeaderboardEntryDto();
            dto.setRank(offset + i + 1);
            dto.setPlayerId(e.getPlayerId());
            dto.setDisplayName(e.getDisplayName());
            dto.setBestScore(e.getBestScore());
            dto.setRegion(e.getRegion());
            dto.setScoreTimestamp(e.getScoreTimestamp());
            dtos.add(dto);
        }
        return dtos;
    }
}
