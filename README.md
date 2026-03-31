# Spec-Driven App

A monorepo demonstrating spec-driven development with AI. Each feature starts as a natural-language description, progresses through a structured specification and planning workflow, and is implemented by an AI coding agent — with consistency analysis and test planning baked into the process before any code is written.

## How It Works

This repo uses [SpecKit](https://github.com/speckit) to enforce a phased development workflow:

1. **Specify** (`/speckit.specify`) — Natural-language input becomes a structured feature spec with user stories, functional requirements, success criteria, and edge cases
2. **Plan** (`/speckit.plan`) — The spec is turned into an implementation plan with architecture decisions, data models, API contracts, and a research document
3. **Tasks** (`/speckit.tasks`) — The plan is decomposed into an ordered task breakdown with dependencies, parallel markers, and test tasks embedded in each user story
4. **Analyze** (`/speckit.analyze`) — Cross-artifact consistency analysis catches contradictions between spec, plan, contracts, data model, and tasks before implementation begins
5. **Implement** (`/speckit.implement`) — The coding agent executes tasks phase-by-phase, running tests as it goes

## Tooling

| Tool | Role |
|------|------|
| **SpecKit v0.4.3** | Spec-driven development framework — structured prompts and quality gates for each phase |
| **GitHub Copilot Chat (Claude Opus 4.6)** | AI coding agent — drives every phase from spec authoring to implementation and runtime debugging |
| **Copilot Skills** | Domain-specific knowledge loaded by the agent (e.g., Azure Cosmos DB best practices with ~2,800 rules) |
| **Azure Cosmos DB Emulator** | Local database for runtime and integration testing |

## Repository Structure

```
spec-driven-app/
├── README.md                 # This file
├── .gitignore
├── .specify/                 # SpecKit configuration, templates, and scripts
├── specs/                    # Feature specifications (one directory per feature)
│   └── 001-game-leaderboard-api/
│       ├── spec.md           # Feature specification
│       ├── plan.md           # Implementation plan
│       ├── tasks.md          # Task breakdown (50 tasks)
│       ├── research.md       # Technology research and decisions
│       ├── data-model.md     # Database schemas and indexes
│       ├── quickstart.md     # Local development guide
│       ├── contracts/        # API contracts
│       ├── checklists/       # Quality checklists
│       ├── README.md         # Feature overview and development journey
│       └── blog-post.md      # Writeup of the spec-driven process
└── apps/                     # Application code (one directory per feature)
    └── 001-game-leaderboard/
        ├── pom.xml           # Maven project
        ├── src/              # Java source and tests
        ├── seed-test-data.py # Cosmos DB test data seeder
        └── emulator-truststore.jks  # Local SSL truststore for emulator
```

Specs and apps are paired by feature number. `specs/001-*` is the design. `apps/001-*` is the code.

## Features

### 001 — Game Leaderboard API

Spring Boot 3 REST API for a mobile game leaderboard backed by Azure Cosmos DB. Score submissions, global/regional leaderboards, player rankings, and profile queries with weekly periods.

- **Spec**: [specs/001-game-leaderboard-api/](specs/001-game-leaderboard-api/)
- **App**: [apps/001-game-leaderboard/](apps/001-game-leaderboard/)
- **Stack**: Java 21, Spring Boot 3.2.5, Spring Data Azure Cosmos DB
- **Tests**: 38 unit + controller tests passing
- **Status**: Implemented and verified against Cosmos DB Emulator

## Getting Started

Each app has its own build and run instructions. See the feature's spec README for details.

```bash
# Build and test the leaderboard API
cd apps/001-game-leaderboard
mvn test

# Run against Cosmos DB Emulator (requires emulator running on localhost:8081)
mvn spring-boot:run "-Dspring-boot.run.profiles=emulator" \
  "-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStore=./emulator-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"
```
