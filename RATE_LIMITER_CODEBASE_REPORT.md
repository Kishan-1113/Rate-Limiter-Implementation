# Rate Limitter Codebase Report

## 1. Purpose and scope

This Spring Boot application exposes four GET endpoints. Each request supplies
a user ID and user tier as URL query parameters. The service selects a limiter
for that tier, and the limiter decides whether the request is accepted.

The HTTP service configuration currently is:

| Tier | Algorithm | Maximum | Window |
| --- | --- | ---: | ---: |
| `FREE` | Fixed window | 5 requests | 60 seconds |
| `PREMIUM` | Sliding-window log | 12 requests | 60 seconds |

These values and algorithms come from `RateLimitService`, not from the enum or
the factory. Limiter classes can also be constructed with different
configurations for tests or future integrations.

## 2. Source file and function guide

### Application entry point

#### `src/main/java/com/example/rate_limitter/RateLimitterApplication.java`

- `main(String[] args)` — Starts the Spring Boot application.
- `RateLimitterApplication` is annotated with `@SpringBootApplication`, which
  enables auto-configuration and component scanning under the
  `com.example.rate_limitter` package.

### HTTP controllers

#### `src/main/java/com/example/rate_limitter/Controllers/controller1.java`

Base route: `/api/v1`.

- Constructor — Receives the shared `RateLimitService` from Spring using
  constructor injection.
- `getMethod1(String userId, UserTier tier)` — Handles `GET /api/v1/c1`,
  reads `userId` and `tier` query parameters, and forwards them to shared
  controller support.
- `getMethod2(String userId, UserTier tier)` — Handles `GET /api/v1/c2` in the
  same way.

#### `src/main/java/com/example/rate_limitter/Controllers/controller2.java`

Base route: `/api/v2`.

- Constructor — Receives the shared `RateLimitService` from Spring.
- `getMethod1(String userId, UserTier tier)` — Handles `GET /api/v2/c1`.
- `getMethod2(String userId, UserTier tier)` — Handles `GET /api/v2/c2`.

Both versions pass requests to the same Spring service bean and therefore use
the same per-tier limiter instances and quota state.

#### `src/main/java/com/example/rate_limitter/Controllers/RateLimitControllerSupport.java`

- Private constructor — Prevents instantiation; this is a utility class.
- `respond(RateLimitService, String, UserTier, String)` — Rejects a blank user
  ID with `400 Bad Request`; builds a `User` and asks the service whether the
  request is allowed; returns `429 Too Many Requests` when denied or `200 OK`
  with the endpoint-specific response text when allowed.

Spring performs query-parameter binding before this method runs. Missing
parameters or a tier that cannot be converted to `FREE`/`PREMIUM` are rejected
as a bad request by Spring MVC.

### Domain model and enums

#### `src/main/java/com/example/rate_limitter/User/User.java`

- Constructor — Creates a user with a `userId` and `UserTier`.
- Lombok `@Getter` and `@Setter` generate accessors at compile time. The class
  is a small request/domain object passed from the controller layer to the
  service layer.

#### `src/main/java/com/example/rate_limitter/Enums/UserTier.java`

- `FREE` and `PREMIUM` — Defines the supported user tiers. The enum is used by
  Spring to convert the `tier` query parameter and by the service to select a
  limiter.

#### `src/main/java/com/example/rate_limitter/Enums/RateLimitType.java`

- `TOKEN_BUCKET`, `FIXED_WINDOW`, `SLIDING_WINDOW`, `LEAKY_BUCKET` — Names the
  limiter strategies recognized by the factory.

### Service and factory

#### `src/main/java/com/example/rate_limitter/RateLimiterService/RateLimitService.java`

- Constructor — Creates and stores one limiter per configured tier:
  - `FREE`: fixed window, 5 requests, 60 seconds.
  - `PREMIUM`: sliding-window log, 12 requests, 60 seconds.
- `allowRequest(User user)` — Validates that the user, ID, and tier are present;
  selects the limiter for the tier; and calls `allowRequest` with the actual
  user ID. This is what makes quotas per-user rather than global or merely per
  tier.

The limiter map is an `EnumMap<UserTier, RateLimiter>`. It is populated during
construction and read by requests; mutable limiter state is held separately
inside each limiter.

#### `src/main/java/com/example/rate_limitter/RateLimiterFactory/RateLimiterFactory.java`

- `createLimiter(RateLimitType, RateLimitConfig)` — Uses a switch expression to
  construct the concrete limiter matching the requested type. The service
  currently constructs its limiters directly through this factory during
  initialization.

#### `src/main/java/com/example/rate_limitter/RateLimiterFactory/RateLimiter.java`

- Constructor — Stores a non-null limiter type and configuration in final
  fields.
- `allowRequest(String userId)` — Abstract operation each algorithm
  implements. Its boolean result indicates whether a request can proceed.

This abstract class provides a common contract for the concrete strategies.

#### `src/main/java/com/example/rate_limitter/RateLimiterFactory/RateLimitConfig.java`

- Constructor — Validates that both the maximum request count and window length
  are greater than zero, then stores them.
- Lombok `@Getter` — Generates read-only accessors. Both fields are final, so
  callers cannot alter an existing limit configuration through setters.

### Limiter implementations

Each limiter stores state by user ID in a `ConcurrentHashMap` and performs
updates inside `compute`, so simultaneous operations for a given key are
serialized by the map operation.

#### `src/main/java/com/example/rate_limitter/RateLimiters/FixedWindowRateLimiter.java`

- Constructor — Identifies the algorithm as `FIXED_WINDOW`.
- `allowRequest(String userId)` — Calculates the current epoch-aligned window,
  then atomically creates or updates that user's window counter. It allows
  requests until the counter reaches `maxRequests`; a new time window starts a
  new counter.
- `WindowCounter` record — Holds the window number and request count together
  as one immutable per-user state value.

#### `src/main/java/com/example/rate_limitter/RateLimiters/SlidingWindowLogRateLimiter.java`

- Constructor — Identifies the algorithm as `SLIDING_WINDOW`.
- `allowRequest(String userId)` — Removes timestamps older than the configured
  window, checks the remaining timestamp count, and records the current time
  when capacity remains. It maintains a rolling window rather than resetting
  all users at a fixed boundary.

#### `src/main/java/com/example/rate_limitter/RateLimiters/TokenBucketLimiter.java`

- Constructor — Identifies the algorithm as `TOKEN_BUCKET`.
- `allowRequest(String userId)` — Refills a user's fractional token balance
  according to elapsed monotonic time, caps the balance at `maxRequests`, and
  consumes one token when available. New users start with a full bucket, minus
  the token consumed for their first request.
- `BucketState` record — Holds the token count and last-refill time as one
  immutable state value.

#### `src/main/java/com/example/rate_limitter/RateLimiters/LeakyBucketLimiter.java`

- Constructor — Identifies the algorithm as `LEAKY_BUCKET`.
- `allowRequest(String userId)` — Reduces the user's bucket level according to
  elapsed monotonic time, then adds the incoming request if the bucket has
  capacity.
- `BucketState` record — Holds the bucket level and last-update time as an
  immutable state value.

### Configuration and build

#### `src/main/resources/application.properties`

- Sets the Spring application name to `rate_limitter`. No rate values are
  currently externalized here.

#### `pom.xml`

- Declares Spring Boot Web MVC, Lombok, and Spring Boot test dependencies.
- Sets Java 21 as the target version.
- Configures Spring Boot packaging and the Lombok annotation processor.

## 3. Request lifecycle

For example:

```text
GET /api/v1/c1?userId=user-123&tier=FREE
```

1. Spring MVC matches `/api/v1/c1` to `controller1.getMethod1`.
2. Spring converts the `userId` string and `tier` enum query parameters into
   method arguments. A missing parameter or invalid enum value results in a
   `400 Bad Request`.
3. The controller calls `RateLimitControllerSupport.respond`, passing the
   service, identity, tier, and success text.
4. The support method rejects a blank ID with `400`; otherwise, it constructs
   a `User` and calls `RateLimitService.allowRequest`.
5. The service validates the object, selects the limiter associated with the
   tier, and calls the limiter with the user's ID.
6. The concrete limiter updates that user's state and returns `true` or
   `false`.
7. The controller support returns `200 OK` on `true`, or `429 Too Many Requests`
   on `false`.

The four routes are `/api/v1/c1`, `/api/v1/c2`, `/api/v2/c1`, and
`/api/v2/c2`. All share the same service and quota state while the application
instance is running.

## 4. Object-oriented design concepts

- **Encapsulation:** Each limiter owns its internal per-user state. Callers use
  the `allowRequest` contract rather than manipulating its maps or counters.
- **Abstraction:** `RateLimiter` presents a shared abstract API without
  exposing the details of any algorithm.
- **Inheritance:** Fixed-window, sliding-window, token-bucket, and leaky-bucket
  implementations extend `RateLimiter`.
- **Polymorphism:** `RateLimitService` stores references to `RateLimiter`, so
  it can route to different concrete limiter classes without algorithm-specific
  branching in its request method.
- **Factory pattern:** `RateLimiterFactory` centralizes construction based on
  `RateLimitType`.
- **Composition / dependency injection:** Controllers depend on
  `RateLimitService`; the service uses a map of limiter objects; the framework
  supplies the shared service to controllers through constructor injection.
- **Enums as constrained values:** `UserTier` and `RateLimitType` avoid
  scattering free-form tier and strategy strings throughout the application.
- **Records for state values:** Limiter state is represented by immutable
  records, which make each state replacement explicit.

## 5. Good practices currently present

- Constructor injection makes the controller's dependency explicit and easy
  to replace in tests.
- Configuration objects validate positive limits and are immutable after
  construction.
- Per-user state is updated with `ConcurrentHashMap.compute`, avoiding
  separate unsynchronized read/modify/write steps for an individual key.
- Time-based refill algorithms use `System.nanoTime()` for elapsed durations;
  wall-clock time is used where an epoch-aligned fixed window is intended.
- The limiter interface keeps HTTP handling separate from algorithm logic.
- HTTP responses distinguish successful requests (`200`), malformed requests
  (`400`), and exceeded quotas (`429`).
- Unit tests cover per-user isolation, factory support for all enum strategies,
  capacity under concurrent calls, and tier selection. A Spring context test
  checks application startup.
- README instructions document endpoint usage and supported query values.

These are useful foundations; they do not by themselves make this application
production-ready.

## 6. Important limitations and follow-up considerations

- **Caller-controlled tier:** `tier` is accepted from the URL. A caller can
  claim `PREMIUM` unless a trusted authentication or user-data layer verifies
  the tier. Production systems should derive tier from authenticated identity,
  not trust a request parameter.
- **In-memory state:** Quotas are local to one application process. They reset
  on restart and are not shared across multiple server instances. A shared
  store or coordinated distributed limiter is needed for multi-instance
  deployments.
- **State cleanup:** Per-user map entries are never evicted. A large number of
  distinct user IDs can grow memory use over time.
- **Input validation and error shape:** Spring handles missing/invalid enum
  conversion, but there is no centralized exception handler or consistent
  structured error response. The service's `IllegalArgumentException` checks
  are internal guardrails and should not be assumed to map to a designed API
  error format.
- **HTTP caching and privacy:** Identity and tier are in the URL, so they may
  appear in access logs, browser history, or monitoring systems. Query
  parameters are used here because that is the requested API shape; production
  identity is generally safer in a trusted authentication context.
- **Operational features:** There is no metrics, audit logging, tracing,
  configurable retry time, `Retry-After` response header, or externalized
  limiter configuration at present.
- **Domain object mutability:** `User` has generated setters. If the value is
  intended only as an immutable request passed to the service, a record or
  final fields would reduce accidental mutation.
- **Naming consistency:** The controller class names currently start with a
  lowercase letter (`controller1`, `controller2`), unlike standard Java class
  naming convention.
- **Automated-test/config mismatch:** The service test in
  `RateLimiterTests.java` still expects 20 FREE requests and asserts that a
  further request is rejected. The current FREE configuration allows only 5,
  so that test fails at runtime until the test expectation or service
  configuration is deliberately aligned. The README has been corrected to
  describe the configuration currently in the service; this report does not
  change the service's limits.

## 7. Tests and verification

Test sources:

- `src/test/java/com/example/rate_limitter/RateLimiterTests.java`
  - `fixedWindowEnforcesLimitPerUser()` — Checks fixed-window capacity and
    separation between two IDs.
  - `eachLimiterTypeIsCreatedAndEnforcesItsCapacity()` — Checks that the
    factory constructs every enum strategy and that each denies after capacity.
  - `concurrentRequestsCannotExceedCapacity()` — Runs concurrent calls and
    checks that exactly the configured capacity is accepted.
  - `serviceUsesUserIdAndTierForSharedControllerQuota()` — Checks service
    routing by tier and user; its FREE threshold is currently stale as noted
    above.
- `src/test/java/com/example/rate_limitter/RateLimitterApplicationTests.java`
  - `contextLoads()` — Starts the Spring application context to detect basic
    wiring or startup failures.

At report creation, `mvnw test` compiles the application and runs all five
tests, but the service test fails because it expects 20 FREE requests while
the service is configured for 5. The other four tests pass. The test or
configuration needs an intentional follow-up alignment before the suite can
pass completely.
