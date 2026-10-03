package com.example.rate_limitter.RateLimiters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.RateLimiterFactory.RateLimitConfig;
import com.example.rate_limitter.RateLimiterFactory.RateLimiter;

public class LeakyBucketLimiter extends RateLimiter {
    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();

    public LeakyBucketLimiter(RateLimitConfig config) {
        super(RateLimitType.LEAKY_BUCKET, config);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = System.nanoTime();
        double leakRate = (double) config.getMaxRequests() / config.getWindowInSeconds();

        buckets.compute(userId, (id, bucket) -> {
            if (bucket == null) {
                allowed.set(true);
                return new BucketState(1.0, now);
            }

            double elapsedSeconds = (now - bucket.lastUpdatedNanos()) / 1_000_000_000.0;
            double currentLevel = Math.max(0.0, bucket.level() - elapsedSeconds * leakRate);
            if (currentLevel + 1.0 <= config.getMaxRequests()) {
                allowed.set(true);
                currentLevel += 1.0;
            }
            return new BucketState(currentLevel, now);
        });
        return allowed.get();
    }

    private record BucketState(double level, long lastUpdatedNanos) {
    }
}
