package com.example.rate_limitter.RateLimiters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.RateLimiterFactory.RateLimitConfig;
import com.example.rate_limitter.RateLimiterFactory.RateLimiter;

public class TokenBucketLimiter extends RateLimiter {
    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();

    public TokenBucketLimiter(RateLimitConfig config) {
        super(RateLimitType.TOKEN_BUCKET, config);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = System.nanoTime();
        double refillRate = (double) config.getMaxRequests() / config.getWindowInSeconds();

        buckets.compute(userId, (id, bucket) -> {
            if (bucket == null) {
                allowed.set(true);
                return new BucketState(config.getMaxRequests() - 1.0, now);
            }

            double elapsedSeconds = (now - bucket.lastRefillNanos()) / 1_000_000_000.0;
            double availableTokens = Math.min(config.getMaxRequests(),
                    bucket.tokens() + elapsedSeconds * refillRate);
            if (availableTokens >= 1.0) {
                allowed.set(true);
                availableTokens -= 1.0;
            }
            return new BucketState(availableTokens, now);
        });
        return allowed.get();
    }

    private record BucketState(double tokens, long lastRefillNanos) {
    }
}
