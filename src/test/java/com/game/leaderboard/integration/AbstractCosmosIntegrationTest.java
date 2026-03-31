package com.game.leaderboard.integration;

import com.game.leaderboard.repository.LeaderboardEntryRepository;
import com.game.leaderboard.repository.PlayerProfileRepository;
import com.game.leaderboard.repository.ScoreEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("emulator")
public abstract class AbstractCosmosIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    protected ScoreEntryRepository scoreEntryRepository;

    @Autowired
    protected PlayerProfileRepository playerProfileRepository;

    @BeforeEach
    void cleanUp() {
        leaderboardEntryRepository.deleteAll();
        scoreEntryRepository.deleteAll();
        playerProfileRepository.deleteAll();
    }
}
