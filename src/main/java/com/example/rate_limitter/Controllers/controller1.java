package com.example.rate_limitter.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.rate_limitter.Enums.UserTier;
import com.example.rate_limitter.RateLimiterService.RateLimitService;

@RestController
@RequestMapping("/api/v1")
public class controller1 {
    private final RateLimitService rateLimitService;

    public controller1(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/c1")
    public ResponseEntity<String> getMethod1(
            @RequestParam String userId,
            @RequestParam UserTier tier) {
        return RateLimitControllerSupport.respond(rateLimitService, userId, tier, "Request through V1/c1");
    }

    @GetMapping("/c2")
    public ResponseEntity<String> getMethod2(
            @RequestParam String userId,
            @RequestParam UserTier tier) {
        return RateLimitControllerSupport.respond(rateLimitService, userId, tier, "Request through V1/c2");
    }
}
