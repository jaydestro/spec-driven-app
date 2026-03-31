package com.game.leaderboard.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import com.game.leaderboard.model.LeaderboardEntry;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardEntryRepository extends CosmosRepository<LeaderboardEntry, String> {

    @Query("SELECT * FROM c WHERE c.periodId = @periodId ORDER BY c.bestScore DESC, c.scoreTimestamp ASC OFFSET @offset LIMIT @limit")
    List<LeaderboardEntry> findTopByPeriod(String periodId, int offset, int limit);

    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.periodId = @periodId")
    long countByPeriod(String periodId);

    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.periodId = @periodId AND c.bestScore > @score")
    long countWithHigherScore(String periodId, long score);

    @Query("SELECT * FROM c WHERE c.periodId = @periodId AND c.region = @region ORDER BY c.bestScore DESC, c.scoreTimestamp ASC OFFSET @offset LIMIT @limit")
    List<LeaderboardEntry> findTopByPeriodAndRegion(String periodId, String region, int offset, int limit);

    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.periodId = @periodId AND c.region = @region")
    long countByPeriodAndRegion(String periodId, String region);

    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.periodId = @periodId AND c.region = @region AND c.bestScore > @score")
    long countWithHigherScoreInRegion(String periodId, String region, long score);

    @Query("SELECT * FROM c WHERE c.periodId = @periodId AND c.playerId = @playerId")
    List<LeaderboardEntry> findByPeriodAndPlayer(String periodId, String playerId);

    @Query("SELECT * FROM c WHERE c.periodId = @periodId AND c.bestScore <= @upperScore ORDER BY c.bestScore DESC, c.scoreTimestamp ASC OFFSET 0 LIMIT @limit")
    List<LeaderboardEntry> findSurroundingAbove(String periodId, long upperScore, int limit);

    @Query("SELECT * FROM c WHERE c.periodId = @periodId AND c.bestScore >= @lowerScore ORDER BY c.bestScore ASC, c.scoreTimestamp DESC OFFSET 0 LIMIT @limit")
    List<LeaderboardEntry> findSurroundingBelow(String periodId, long lowerScore, int limit);
}
