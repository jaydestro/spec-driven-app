package com.game.leaderboard.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.game.leaderboard.model.PlayerProfile;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerProfileRepository extends CosmosRepository<PlayerProfile, String> {
}
