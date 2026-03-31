package com.game.leaderboard.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.game.leaderboard.model.ScoreEntry;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreEntryRepository extends CosmosRepository<ScoreEntry, String> {
}
