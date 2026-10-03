package com.example.rate_limitter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.Enums.UserTier;
import com.example.rate_limitter.RateLimiterFactory.RateLimitConfig;
import com.example.rate_limitter.RateLimiterFactory.RateLimiter;
import com.example.rate_limitter.RateLimiterFactory.RateLimiterFactory;
import com.example.rate_limitter.RateLimiterService.RateLimitService;
import com.example.rate_limitter.User.User;

class RateLimiterTests {
    @Test
    void fixedWindowEnforcesLimitPerUser() {
        RateLimiter limiter = RateLimiterFactory.createLimiter(
                RateLimitType.FIXED_WINDOW, new RateLimitConfig(2, 60));

        assertTrue(limiter.allowRequest("user-1"));
        assertTrue(limiter.allowRequest("user-1"));
        assertFalse(limiter.allowRequest("user-1"));
        assertTrue(limiter.allowRequest("user-2"));
    }

    @Test
    void eachLimiterTypeIsCreatedAndEnforcesItsCapacity() {
        for (RateLimitType type : RateLimitType.values()) {
            RateLimiter limiter = RateLimiterFactory.createLimiter(type, new RateLimitConfig(2, 60));

            assertTrue(limiter.allowRequest("user-1"), type.name());
            assertTrue(limiter.allowRequest("user-1"), type.name());
            assertFalse(limiter.allowRequest("user-1"), type.name());
        }
    }

    @Test
    void concurrentRequestsCannotExceedCapacity() {
        for (RateLimitType type : RateLimitType.values()) {
            RateLimiter limiter = RateLimiterFactory.createLimiter(type, new RateLimitConfig(2, 60));
            long allowedRequests = java.util.stream.IntStream.range(0, 64)
                    .parallel()
                    .filter(request -> limiter.allowRequest("concurrent-user"))
                    .count();

            assertEquals(2, allowedRequests, type.name());
        }
    }

    @Test
    void serviceUsesUserIdAndTierForSharedControllerQuota() {
        RateLimitService service = new RateLimitService();
        User freeUser = new User("shared-user", UserTier.FREE);

        for (int i = 0; i < 20; i++) {
            assertTrue(service.allowRequest(freeUser));
        }
        assertFalse(service.allowRequest(freeUser));
        assertTrue(service.allowRequest(new User("another-user", UserTier.FREE)));
        assertTrue(service.allowRequest(new User("shared-user", UserTier.PREMIUM)));
    }
}
