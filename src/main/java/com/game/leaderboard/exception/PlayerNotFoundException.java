package com.game.leaderboard.exception;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String playerId) {
        super("Player with ID '" + playerId + "' does not exist");
    }
}
