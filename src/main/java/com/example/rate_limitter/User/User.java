package com.example.rate_limitter.User;

import com.example.rate_limitter.Enums.UserTier;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private String userId;
    private UserTier tier;

    public User(String userId, UserTier tier) {
        this.userId = userId;
        this.tier = tier;
    }
}
