package com.example.rate_limitter.RateLimiterFactory;

import lombok.Getter;

@Getter
public class RateLimitConfig {
    private final int maxRequests;
    private final int windowInSeconds;

    public RateLimitConfig(int reqs, int windowLenInSec) {
        if (reqs <= 0) {
            throw new IllegalArgumentException("maxRequests must be greater than zero");
        }
        if (windowLenInSec <= 0) {
            throw new IllegalArgumentException("windowInSeconds must be greater than zero");
        }
        this.maxRequests = reqs;
        this.windowInSeconds = windowLenInSec;
    }
}
