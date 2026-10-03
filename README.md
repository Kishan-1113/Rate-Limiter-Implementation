# Rate Limitter

The application rate-limits requests per user ID and tier. All four limiter types
(`FIXED_WINDOW`, `SLIDING_WINDOW`, `TOKEN_BUCKET`, and `LEAKY_BUCKET`) can be
created through `RateLimiterFactory`; the HTTP endpoints currently use fixed
windows: 20 requests per minute for `FREE` users and 50 per minute for
`PREMIUM` users.

## Try it with Postman

Send a `GET` request to any endpoint with `userId` and `tier` query parameters:

- `http://localhost:8080/api/v1/c1?userId=user-123&tier=FREE`
- `http://localhost:8080/api/v1/c2?userId=user-123&tier=FREE`
- `http://localhost:8080/api/v2/c1?userId=user-123&tier=FREE`
- `http://localhost:8080/api/v2/c2?userId=user-123&tier=FREE`

Use `FREE` or `PREMIUM` for `tier`. These values are case-sensitive.

An allowed request returns `200 OK`. Once that user reaches the tier's limit
within the current fixed window, requests return `429 Too Many Requests`.
Requests to all four endpoints share the same per-user, per-tier quota. Missing
query parameters or an invalid tier are rejected with `400 Bad Request`.