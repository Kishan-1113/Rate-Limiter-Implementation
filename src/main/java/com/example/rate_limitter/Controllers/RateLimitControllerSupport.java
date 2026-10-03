package com.example.rate_limitter.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.rate_limitter.Enums.UserTier;
import com.example.rate_limitter.RateLimiterService.RateLimitService;
import com.example.rate_limitter.User.User;

final class RateLimitControllerSupport {
    private RateLimitControllerSupport() {
    }

    static ResponseEntity<String> respond(
            RateLimitService rateLimitService,
            String userId,
            UserTier tier,
            String successBody) {
        if (userId.isBlank()) {
                return ResponseEntity.badRequest().body("userId query parameter must not be blank");
        }
        if (!rateLimitService.allowRequest(new User(userId, tier))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        }
        return ResponseEntity.ok(successBody);
    }
}
