# Rate Limitter

The application rate-limits requests per user ID and tier. All four limiter types
(`FIXED_WINDOW`, `SLIDING_WINDOW`, `TOKEN_BUCKET`, and `LEAKY_BUCKET`) can be
created through `RateLimiterFactory`; the HTTP endpoints currently use fixed
windows: 20 requests per minute for `FREE` users and 50 per minute for
`PREMIUM` users.

## Try it with Postman

Send a `GET` request to any of:

- `http://localhost:8080/api/v1/c1`
- `http://localhost:8080/api/v1/c2`
- `http://localhost:8080/api/v2/c1`
- `http://localhost:8080/api/v2/c2`

Include these headers:

| Header | Example |
| --- | --- |
| `X-User-Id` | `user-123` |
| `X-User-Tier` | `FREE` or `PREMIUM` |

An allowed request returns `200 OK`. Once that user reaches the tier's limit
within the current fixed window, requests return `429 Too Many Requests`.
Requests to all four endpoints share the same per-user, per-tier quota. Missing
or invalid headers are rejected with `400 Bad Request`.