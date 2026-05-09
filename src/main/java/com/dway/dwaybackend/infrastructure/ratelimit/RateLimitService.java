package com.dway.dwaybackend.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.rate-limit.login.capacity:5}")
    private long loginCapacity;

    @Value("${app.rate-limit.login.window-seconds:900}")
    private long loginWindow;

    @Value("${app.rate-limit.auth.capacity:10}")
    private long authCapacity;

    @Value("${app.rate-limit.auth.window-seconds:60}")
    private long authWindow;

    @Value("${app.rate-limit.ai.capacity:20}")
    private long aiCapacity;

    @Value("${app.rate-limit.ai.window-seconds:3600}")
    private long aiWindow;

    public boolean tryConsume(String ip, String path) {
        String key = "ratelimit:" + ip + ":" + getCategory(path);
        long limit = getLimit(path);
        long window = getWindow(path);

        Long count = redisTemplate.opsForValue().increment(key);

        // Set TTL only on first request — when counter is created
        if (count != null && count == 1) {
            redisTemplate.expire(key, window, TimeUnit.SECONDS);
        }

        return count != null && count <= limit;
    }

    private String getCategory(String path) {
        if (path.contains("/auth/login"))  return "login";
        if (path.startsWith("/api/v1/auth/")) return "auth";
        if (path.startsWith("/api/v1/mobile/ai/")) return "ai";
        return "general";
    }

    private long getLimit(String path) {
        if (path.contains("/auth/login"))  return loginCapacity;
        if (path.startsWith("/api/v1/auth/")) return authCapacity;
        if (path.startsWith("/api/v1/mobile/ai/")) return aiCapacity;
        return 100;
    }

    private long getWindow(String path) {
        if (path.contains("/auth/login"))  return loginWindow;
        if (path.startsWith("/api/v1/auth/")) return authWindow;
        if (path.startsWith("/api/v1/mobile/ai/")) return aiWindow;
        return 60;
    }
}