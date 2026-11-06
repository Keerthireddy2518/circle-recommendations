# Technical Design

## Executive summary

This project implements a small but production-aware recommendation service for Lean In members. The goal is to help a user discover circles that are relevant to their interests, active enough to be valuable, and available for new members. The design prioritizes explainability, transparent scoring, and operational simplicity over a heavily optimized recommendation stack.

The system is built as a layered Spring Boot service using JPA and H2 for local execution. At startup, it seeds the database from the provided JSON files and exposes a REST endpoint that returns a ranked list of circles along with short, human-readable reasons for each recommendation.

## System architecture

The implementation follows a clean layered structure:

1. `bootstrap`
   - `JsonDataLoader` reads the source JSON files and persists them into H2 in a safe order.
2. `domain.entity`
   - `User`, `Circle`, `Topic`, `Membership`, and `UserActivity` model the domain relationships.
3. `domain.repository`
   - Spring Data JPA repositories provide data access and query hooks for membership, activity, topic, and circle lookups.
4. `service`
   - `CircleRecommendationService` orchestrates candidate discovery, filtering, deduplication, and ranking.
   - `CircleRecommendationScorer` computes the weighted recommendation score.
   - `RecommendationScoringProperties` centralizes thresholds and weights for easy tuning.
5. `api`
   - `UserRecommendationsController` exposes the recommendation endpoint.
   - `GlobalExceptionHandler` returns consistent HTTP error payloads.

## Data model and schema

The core schema is intentionally simple and explicit:

- `users`
  - primary key: `id`
  - name and email metadata
  - reference to related topic and activity records
- `circles`
  - primary key: `id`
  - name, description, active flag, creation timestamp
- `topics`
  - primary key: `id`
  - unique topic name for canonical topic matching
- `memberships`
  - primary key: `id`
  - composite relationship between user and circle
  - role, status, and join date
- `user_activities`
  - user and optional circle references
  - event type, timestamp, and details payload

Key design decisions:

- topic and user-interest normalization is handled centrally so the same topic name resolves consistently across users, circles, and the master topic list
- active membership and activity records are treated as first-class signals in recommendation ranking
- the schema keeps the relationships explicit rather than embedding data in denormalized strings, which makes filtering and scoring easier to reason about

### Indexing and retrieval

The schema includes targeted indexes to support the recommendation path and operational lookup patterns:

- user lookup by email and ID
- circle lookup by name and active status
- membership uniqueness by `user_id + circle_id`
- recent user and circle activity queries by time order
- event type filtering for future analytics or reporting

These are lightweight and appropriate for the scale of this assignment while still keeping the recommendation path efficient.

## Data loading strategy

The startup loader is designed to preserve relational integrity and avoid duplicate records on restart:

1. topics are loaded first
2. users are loaded next
3. circles are then created
4. memberships are attached to existing users and circles
5. activities are persisted last

This ordering prevents invalid references and ensures that all foreign keys resolve cleanly. The loader also validates the actual dataset counts and logs a summary so startup integrity is visible in logs.

## Recommendation scoring strategy

The scoring model is deliberately transparent and configurable. Candidate circles are scored using a weighted mix of meaningful signals:

- topic match between a member’s interest set and a circle’s topic set
- recent circle engagement and activity volume
- the user’s own history of engagement with the circle
- recency of the most recent activity
- circle popularity and member availability
- cold-start fallback logic when a user has limited or no prior data

The system also filters out ineligible circles before ranking:

- circles the user already belongs to
- circles that are full
- circles that are inactive beyond a configured threshold

To avoid presenting a user with several near-identical circles, the service compares tag overlap and drops redundant candidates above a configurable similarity threshold.

## Explanation model

Each recommendation includes human-readable reasons derived from the actual signals used in scoring. This is important because the recommendation is not only ranking; it is also explaining why the suggestion makes sense to a member.

Examples include:

- strong topic alignment
- recent circle activity
- relationship to the user’s engagement history
- active and available membership pool
- cold-start support when explicit interests are absent

This improves trust and makes the output easier to debug during review or production tuning.

## Tradeoffs and design rationale

This implementation makes a few deliberate tradeoffs:

- it favors explainability over black-box optimization
- it uses simple-but-sensible heuristic ranking rather than a complex ML pipeline
- it keeps the data model relational and understandable rather than introducing unnecessary denormalization
- it uses H2 in-memory persistence for local execution, which is excellent for assignments but not the final production scale model

These choices are appropriate for a reviewable, assignment-sized service where clarity and correctness matter as much as raw sophistication.

## Production evolution

To move this beyond the current assignment, the next logical improvements would be:

1. use Flyway or Liquibase for migration-based schema management
2. add caching for high-frequency recommendation requests and topic lookups
3. materialize aggregated circle activity and membership summaries for faster ranking
4. add monitoring for request latency, ranking quality, and data drift
5. support asynchronous activity ingestion and near-real-time updates
6. strengthen authorization and validation around API access
7. expand the evaluation framework to cover edge cases and zero-result scenarios

## Verified outcome

The final implementation was verified with a fresh Maven run:

```bash
./mvnw test
```

The last validated result reported:

- 12 tests run
- 0 failures
- 0 errors
- 0 skipped
- BUILD SUCCESS

This means the application starts successfully, the seeded data loads correctly, and the recommendation scenarios are covered by automated validation.
