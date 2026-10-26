# Build 09 — Enrollment Is Transactional

Java 21 · Spring Boot 4.0.0 · Spring Data JPA · Flyway · H2 · JUnit 5

## Run

```bash
java -version
mvn clean verify
mvn spring-boot:run
```

H2 is file-backed at `./data/crs.mv.db` (created on first run). No MySQL or Docker needed.
H2 console: `http://localhost:8080/h2-console` with JDBC URL
`jdbc:h2:file:./data/crs;DB_CLOSE_ON_EXIT=FALSE`, user `sa`, blank password.
Keep H2 console disabled in production; this is a local lab.

Tests use a separate in-memory H2 database and Flyway migration. `ddl-auto: validate`
ensures the JPA mapping matches the schema.

## API examples

Create a course:

```http
POST /api/courses
Content-Type: application/json

{"code":"CS101","prerequisiteIds":[]}
```

Create a term (UTC):

```http
POST /api/terms
Content-Type: application/json

{"name":"Fall 2026","registrationOpens":"2026-01-01T00:00:00","registrationCloses":"2027-01-01T00:00:00"}
```

Create two students with `POST /api/students`, body `{"name":"Ada"}`.
Create a section with `POST /api/sections`, body `{"courseId":1,"termId":1,"capacity":1}`.
Enroll with `POST /api/enrollments`, body `{"studentId":1,"sectionId":1}`.

Other routes: `GET /api/courses`, `/api/courses/{id}`, `/api/terms`,
`/api/terms/{id}`, `/api/students`, `/api/students/{id}`,
`/api/sections`, `/api/sections/{id}`, `/api/students/{id}/enrollments`.

## Invariants

1. A term's registration window is inclusive and evaluated in UTC.
2. An enrollment for the same student and section is unique.
3. Every prerequisite course requires a COMPLETED enrollment in any of its sections.
4. SeatAvailabilityPolicy checks the current section snapshot.
5. Enrollment is inserted and flushed.
6. Section seatsTaken is incremented and flushed, engaging `@Version`.

`@Transactional(rollbackFor = Exception.class)` covers checked and unchecked failures.
A conflicting version update or a database constraint violation aborts the transaction.
`ApiExceptionHandler` returns RFC 9457 ProblemDetail (409 for conflicts).

## Tests

`EnrollmentIntegrationTest` verifies checked-exception rollback at steps 5 and 6,
window rejection, and **10 consecutive** two-thread races with CountDownLatch.
Each race asserts exactly one committed enrollment, one failed request,
`seatsTaken == capacity == 1`, and `version == 1`.
`EnrollmentApiTest` checks representative endpoints and error shapes.

## Submission

```bash
git add .
git commit -m "Build 09: Enrollment Is Transactional"
git push
git tag build-09
git push origin build-09
```

**Only submit the tag after `mvn clean verify` succeeds locally and CI is green.**
