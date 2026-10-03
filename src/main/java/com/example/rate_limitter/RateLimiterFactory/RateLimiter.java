package com.example.rate_limitter.RateLimiterFactory;

import com.example.rate_limitter.Enums.RateLimitType;

import java.util.Objects;

public abstract class RateLimiter {

    protected final RateLimitType type;
    protected final RateLimitConfig config;

    protected RateLimiter(RateLimitType type, RateLimitConfig config) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public abstract boolean allowRequest(String userId);
}
