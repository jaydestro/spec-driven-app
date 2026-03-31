package com.game.leaderboard.controller;

import com.game.leaderboard.dto.LeaderboardResponse;
import com.game.leaderboard.dto.PlayerRankResponse;
import com.game.leaderboard.dto.PlayerRankResponse.SurroundingPlayerDto;
import com.game.leaderboard.exception.PlayerNotFoundException;
import com.game.leaderboard.model.LeaderboardEntry;
import com.game.leaderboard.service.LeaderboardService;
import com.game.leaderboard.service.PeriodUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/api/leaderboards/global")
    public ResponseEntity<LeaderboardResponse> getGlobalLeaderboard(
            @RequestParam(required = false) String periodId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        validatePagination(page, pageSize);
        LeaderboardResponse response = leaderboardService.getGlobalLeaderboard(periodId, page, pageSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/leaderboards/regions/{region}")
    public ResponseEntity<LeaderboardResponse> getRegionalLeaderboard(
            @PathVariable String region,
            @RequestParam(required = false) String periodId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        validatePagination(page, pageSize);
        LeaderboardResponse response = leaderboardService.getRegionalLeaderboard(region, periodId, page, pageSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/leaderboards/players/{playerId}/rank")
    public ResponseEntity<PlayerRankResponse> getPlayerRank(
            @PathVariable String playerId,
            @RequestParam(required = false) String periodId,
            @RequestParam(defaultValue = "global") String scope,
            @RequestParam(defaultValue = "10") int surrounding) {
        if (periodId == null || periodId.isBlank()) {
            periodId = PeriodUtil.getCurrentPeriod();
        }

        LeaderboardEntry entry = leaderboardService.findPlayerEntry(periodId, playerId);
        if (entry == null) {
            throw new PlayerNotFoundException(playerId);
        }

        long rank;
        if ("regional".equalsIgnoreCase(scope)) {
            rank = leaderboardService.getRegionalRank(periodId, entry.getRegion(), entry.getBestScore());
        } else {
            rank = leaderboardService.getGlobalRank(periodId, entry.getBestScore());
        }

        List<LeaderboardEntry> surroundingEntries = leaderboardService
                .getSurroundingPlayers(periodId, entry.getBestScore(), surrounding);

        List<SurroundingPlayerDto> surroundingDtos = new ArrayList<>();
        for (LeaderboardEntry e : surroundingEntries) {
            SurroundingPlayerDto dto = new SurroundingPlayerDto();
            long entryRank = leaderboardService.getGlobalRank(periodId, e.getBestScore());
            dto.setRank(entryRank);
            dto.setPlayerId(e.getPlayerId());
            dto.setDisplayName(e.getDisplayName());
            dto.setBestScore(e.getBestScore());
            dto.setRegion(e.getRegion());
            surroundingDtos.add(dto);
        }

        PlayerRankResponse response = new PlayerRankResponse();
        response.setPlayerId(playerId);
        response.setDisplayName(entry.getDisplayName());
        response.setBestScore(entry.getBestScore());
        response.setRank(rank);
        response.setScope(scope);
        response.setPeriodId(periodId);
        response.setSurroundingPlayers(surroundingDtos);

        return ResponseEntity.ok(response);
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be >= 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("PageSize must be between 1 and 100");
        }
    }
}
