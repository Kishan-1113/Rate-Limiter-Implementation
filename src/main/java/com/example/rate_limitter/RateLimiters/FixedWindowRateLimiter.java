package com.example.rate_limitter.RateLimiters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.RateLimiterFactory.RateLimitConfig;
import com.example.rate_limitter.RateLimiterFactory.RateLimiter;

public class FixedWindowRateLimiter extends RateLimiter {

    private final Map<String, WindowCounter> requestCount = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(RateLimitConfig config) {
        super(RateLimitType.FIXED_WINDOW, config);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);

        long windowLengthMillis = config.getWindowInSeconds() * 1000L;
        long currentWindow = Math.floorDiv(System.currentTimeMillis(), windowLengthMillis);

        requestCount.compute(userId, (id, counter) -> {
            if (counter == null || counter.window() != currentWindow) {
                allowed.set(true);
                return new WindowCounter(currentWindow, 1);
            }

            if (counter.count() < config.getMaxRequests()) {
                allowed.set(true);
                return new WindowCounter(currentWindow, counter.count() + 1);
            }
            return counter;
        });

        return allowed.get();
    }

    private record WindowCounter(long window, int count) {
    }
}
