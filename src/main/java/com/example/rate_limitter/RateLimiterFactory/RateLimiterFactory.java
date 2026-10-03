package com.example.rate_limitter.RateLimiterFactory;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.RateLimiters.FixedWindowRateLimiter;
import com.example.rate_limitter.RateLimiters.LeakyBucketLimiter;
import com.example.rate_limitter.RateLimiters.SlidingWindowLogRateLimiter;
import com.example.rate_limitter.RateLimiters.TokenBucketLimiter;

public class RateLimiterFactory {

    public static RateLimiter createLimiter(RateLimitType rateLimitType, RateLimitConfig config) {

        return switch (rateLimitType) {
            case TOKEN_BUCKET -> new TokenBucketLimiter(config);
            case FIXED_WINDOW -> new FixedWindowRateLimiter(config);
            case SLIDING_WINDOW -> new SlidingWindowLogRateLimiter(config);
            case LEAKY_BUCKET -> new LeakyBucketLimiter(config);
        };
    }
}