package com.game.leaderboard.controller;

import com.game.leaderboard.dto.ScoreSubmissionRequest;
import com.game.leaderboard.dto.ScoreSubmissionResponse;
import com.game.leaderboard.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/api/scores")
    public ResponseEntity<ScoreSubmissionResponse> submitScore(
            @Valid @RequestBody ScoreSubmissionRequest request) {
        ScoreSubmissionResponse response = scoreService.submitScore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
