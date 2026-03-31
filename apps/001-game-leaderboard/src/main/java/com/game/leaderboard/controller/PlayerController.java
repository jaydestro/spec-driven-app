package com.game.leaderboard.controller;

import com.game.leaderboard.dto.PlayerProfileResponse;
import com.game.leaderboard.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/api/players/{playerId}")
    public ResponseEntity<PlayerProfileResponse> getPlayerProfile(@PathVariable String playerId) {
        PlayerProfileResponse response = playerService.getPlayerProfile(playerId);
        return ResponseEntity.ok(response);
    }
}
