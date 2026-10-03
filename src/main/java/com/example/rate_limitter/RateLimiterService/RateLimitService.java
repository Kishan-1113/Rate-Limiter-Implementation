package com.example.rate_limitter.RateLimiterService;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.Enums.UserTier;
import com.example.rate_limitter.RateLimiterFactory.RateLimitConfig;
import com.example.rate_limitter.RateLimiterFactory.RateLimiter;
import com.example.rate_limitter.RateLimiterFactory.RateLimiterFactory;
import com.example.rate_limitter.User.User;

@Service
public class RateLimitService {
    private final Map<UserTier, RateLimiter> rateLimiters = new EnumMap<>(UserTier.class);

    public RateLimitService() {

        // 20 reqs/min
        rateLimiters.put(UserTier.FREE,
                RateLimiterFactory.createLimiter(RateLimitType.FIXED_WINDOW,
                        new RateLimitConfig(5, 60)));

        // 50reqs/min
        rateLimiters.put(UserTier.PREMIUM,
                RateLimiterFactory.createLimiter(RateLimitType.SLIDING_WINDOW,
                        new RateLimitConfig(12, 60)));
    }

    public boolean allowRequest(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (user.getTier() == null) {
            throw new IllegalArgumentException("user tier must not be null");
        }

        RateLimiter limiter = rateLimiters.get(user.getTier());

        if (limiter == null)
            throw new IllegalArgumentException("No Rate Limiters configured for this User Tier");

        return limiter.allowRequest(user.getUserId());
    }
}
