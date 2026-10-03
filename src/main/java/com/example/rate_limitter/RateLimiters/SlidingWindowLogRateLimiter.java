package com.example.rate_limitter.RateLimiters;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.rate_limitter.Enums.RateLimitType;
import com.example.rate_limitter.RateLimiterFactory.RateLimitConfig;
import com.example.rate_limitter.RateLimiterFactory.RateLimiter;

public class SlidingWindowLogRateLimiter extends RateLimiter {
    private final Map<String, Queue<Long>> requestLog = new ConcurrentHashMap<>();

    public SlidingWindowLogRateLimiter(RateLimitConfig config) {
        super(RateLimitType.SLIDING_WINDOW, config);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = System.currentTimeMillis();
        long windowLengthMillis = config.getWindowInSeconds() * 1000L;

        requestLog.compute(userId, (id, log) -> {
            if (log == null)
                log = new ArrayDeque<>();

            while (!log.isEmpty() && (now - log.peek()) >= windowLengthMillis) {
                log.poll();
            }

            if (log.size() < config.getMaxRequests()) {
                log.add(now);
                allowed.set(true);
            }
            return log;
        });
        return allowed.get();
    }
}
