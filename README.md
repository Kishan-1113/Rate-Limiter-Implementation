# Rate Limitter

The application rate-limits requests per user ID and tier. All four limiter types
(`FIXED_WINDOW`, `SLIDING_WINDOW`, `TOKEN_BUCKET`, and `LEAKY_BUCKET`) can be
created through `RateLimiterFactory`. The HTTP endpoints currently allow 5
requests per 60 seconds for `FREE` users using a fixed window, and 12 requests
per 60 seconds for `PREMIUM` users using a sliding window.

## Try it with Postman

Send a `GET` request to any endpoint with `userId` and `tier` query parameters:

- `http://localhost:8080/api/v1/c1?userId=user-123&tier=FREE`
- `http://localhost:8080/api/v1/c2?userId=user-123&tier=FREE`
- `http://localhost:8080/api/v2/c1?userId=user-123&tier=FREE`
- `http://localhost:8080/api/v2/c2?userId=user-123&tier=FREE`

Use `FREE` or `PREMIUM` for `tier`. These values are case-sensitive.

An allowed request returns `200 OK`. Once that user reaches the tier's limit
within its configured window, requests return `429 Too Many Requests`.
Requests to all four endpoints share the same per-user, per-tier quota. Missing
query parameters or an invalid tier are rejected with `400 Bad Request`.

See [RATE_LIMITER_CODEBASE_REPORT.md](./RATE_LIMITER_CODEBASE_REPORT.md) for
the file-by-file codebase explanation, request flow, design concepts, and
known limitations.