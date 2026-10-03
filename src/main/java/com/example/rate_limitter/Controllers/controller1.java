package com.example.rate_limitter.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.rate_limitter.Enums.UserTier;
import com.example.rate_limitter.RateLimiterService.RateLimitService;
import com.example.rate_limitter.User.User;

@RestController
@RequestMapping("/api/v1")
public class controller1 {
    private final RateLimitService rateLimitService;

    private static final Logger log = LoggerFactory.getLogger(controller1.class);

    public controller1(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/c1")
    public ResponseEntity<String> getMethod1(
            @RequestParam String userId,
            @RequestParam UserTier tier) {
        return handleRequest(userId, tier, "Request through V1/c1");
    }

    @GetMapping("/c2")
    public ResponseEntity<String> getMethod2(
            @RequestParam String userId,
            @RequestParam UserTier tier) {
        return handleRequest(userId, tier, "Request through V1/c2");
    }

    private ResponseEntity<String> handleRequest(String userId, UserTier tier, String successBody) {
        log.info("Request : userId={} tier={}", userId, tier);

        ResponseEntity<String> response;
        if (userId.isBlank()) {
            response = ResponseEntity.badRequest().body("userId query parameter must not be blank");
        } else if (!rateLimitService.allowRequest(new User(userId, tier))) {
            response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        } else {
            response = ResponseEntity.ok(successBody);
        }

        log.info("Response : userId={} with status={}", userId, response.getStatusCode().value());
        return response;
    }
}
