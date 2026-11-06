# Circle Recommendations API

This project delivers a Spring Boot recommendation service for Lean In Circle discovery. It ranks candidate circles for a given member using real signals from the seeded dataset: topic overlap, recent activity, engagement history, member availability, and recency of activity. The result is a ranked list with short, member-facing explanations that explain why a circle was recommended.

## What this submission includes

- REST endpoint for user-to-circle recommendations
- JPA-backed schema for users, circles, memberships, topics, and activity
- Startup data loader that reads the seeded JSON files and persists relationships safely
- Filtering for existing memberships, full circles, and inactive circles
- Duplicate-circle reduction for near-identical recommendations
- Transparent scoring model with explainable reasons
- Integration coverage for the primary assignment scenarios

## Prerequisites

- Java 21
- Git
- Terminal access in the project root
- Optional: curl or Postman

This project includes the Maven wrapper, so a separate local Maven installation is not required.

## Setup and run

1. Clone the repository.
2. Open a terminal in the project root.
3. Run:

```bash
./mvnw clean test
```

4. Start the app:

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

### H2 console

The in-memory H2 database console is enabled at:

- `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:circle-recommendations`
- Username: `sa`
- Password: empty

## API

### Get recommendations for a user

```http
GET /api/v1/users/{userId}/recommendations
```

Optional query parameter:

- `limit` — maximum number of recommendations to return

Example:

```bash
curl "http://localhost:8080/api/v1/users/42/recommendations?limit=3"
```

Example response:

```json
{
  "userId": 42,
  "recommendations": [
    {
      "circleId": 12,
      "name": "Women in Leadership",
      "score": 0.87,
      "reasons": [
        "Strong topic match on leadership and mentorship.",
        "Recent activity in the circle indicates active engagement.",
        "The circle is currently open and available for new members."
      ]
    }
  ]
}
```

### Error handling

The API returns structured error payloads for invalid or missing requests:

- `400 Bad Request` for invalid parameters
- `404 Not Found` when a user is missing
- `500 Internal Server Error` for unexpected runtime failures

## Seed data and startup loading

The application loads the following five JSON files from `src/main/resources/data`:

- `topics.json`
- `users.json`
- `circles.json`
- `memberships.json`
- `activity.json`

The loader runs automatically at startup, preserves foreign-key relationships, avoids duplicate inserts on restarts, and logs a validation summary that confirms the actual records loaded.

## Recommendation approach

The scoring model emphasizes explainability and product usefulness rather than a black-box ranking algorithm. Candidate circles are filtered to exclude:

- circles the user already belongs to
- circles at capacity
- circles inactive for more than the configured threshold

Remaining candidates are ranked by a weighted mix of:

- topic overlap between user interests and circle topics
- recent activity within the circle
- member engagement signals
- recency of activity
- membership availability
- support for cold-start users with limited prior data

## Verification

Run the full test suite with:

```bash
./mvnw test
```

Verified locally in the final run:

- 12 tests run
- 0 failures
- 0 errors
- 0 skipped
- BUILD SUCCESS

The suite covers the assignment scenarios for users 42, 7, 15, and 23, plus scorer and application smoke tests.

## Submission notes

This implementation is designed for a clean handoff: the service is runnable locally, the dataset is real and persisted in H2, the recommendation logic is transparent, and the API output is easy to inspect and explain during review.
