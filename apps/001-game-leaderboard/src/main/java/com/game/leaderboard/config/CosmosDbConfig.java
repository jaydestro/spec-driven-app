package com.game.leaderboard.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.cosmos.GatewayConnectionConfig;
import com.azure.cosmos.models.CompositePath;
import com.azure.cosmos.models.CompositePathSortOrder;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.ExcludedPath;
import com.azure.cosmos.models.IncludedPath;
import com.azure.cosmos.models.IndexingPolicy;
import com.azure.cosmos.models.ThroughputProperties;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCosmosRepositories(basePackages = "com.game.leaderboard.repository")
public class CosmosDbConfig extends AbstractCosmosConfiguration {

    @Value("${azure.cosmos.endpoint}")
    private String endpoint;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database}")
    private String database;

    @Value("${azure.cosmos.connection-mode:direct}")
    private String connectionMode;

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .contentResponseOnWriteEnabled(true);

        if ("gateway".equalsIgnoreCase(connectionMode)) {
            builder.gatewayMode(GatewayConnectionConfig.getDefaultConfig());
        } else {
            builder.directMode(DirectConnectionConfig.getDefaultConfig());
        }

        return builder;
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Bean
    ApplicationRunner initializeContainers(CosmosClientBuilder cosmosClientBuilder) {
        return args -> {
            try (CosmosClient client = cosmosClientBuilder.buildClient()) {
                client.createDatabaseIfNotExists(database);
                CosmosDatabase db = client.getDatabase(database);

                initLeaderboardEntriesContainer(db);
                initScoresContainer(db);
                initPlayersContainer(db);
            }
        };
    }

    private void initLeaderboardEntriesContainer(CosmosDatabase db) {
        CosmosContainerProperties props = new CosmosContainerProperties("leaderboard-entries", "/periodId");

        CompositePath scorePath = new CompositePath();
        scorePath.setPath("/bestScore");
        scorePath.setOrder(CompositePathSortOrder.DESCENDING);

        CompositePath timestampPath = new CompositePath();
        timestampPath.setPath("/scoreTimestamp");
        timestampPath.setOrder(CompositePathSortOrder.ASCENDING);

        IndexingPolicy indexingPolicy = props.getIndexingPolicy();
        indexingPolicy.setIncludedPaths(List.of(new IncludedPath("/*")));
        indexingPolicy.setExcludedPaths(List.of(new ExcludedPath("/displayName/?"), new ExcludedPath("/\"_etag\"/?")));
        indexingPolicy.setCompositeIndexes(List.of(List.of(scorePath, timestampPath)));

        db.createContainerIfNotExists(props, ThroughputProperties.createManualThroughput(400));
    }

    private void initScoresContainer(CosmosDatabase db) {
        CosmosContainerProperties props = new CosmosContainerProperties("scores", "/playerId");
        db.createContainerIfNotExists(props, ThroughputProperties.createManualThroughput(400));
    }

    private void initPlayersContainer(CosmosDatabase db) {
        CosmosContainerProperties props = new CosmosContainerProperties("players", "/playerId");
        db.createContainerIfNotExists(props, ThroughputProperties.createManualThroughput(400));
    }
}
