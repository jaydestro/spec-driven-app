package com.game.leaderboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ScoreSubmissionRequest {

    @NotBlank(message = "playerId is required")
    private String playerId;

    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 999999999, message = "Score must not exceed 999,999,999")
    private long score;

    public ScoreSubmissionRequest() {
    }

    public ScoreSubmissionRequest(String playerId, long score) {
        this.playerId = playerId;
        this.score = score;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }
}
