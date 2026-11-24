package com.opspilot.controlplane.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opspilot.cache.enabled", havingValue = "true")
@Tag(name = "Cache Management", description = "Redis cache inspection and eviction (admin only)")
public class CacheManagementController {

    private final CacheManager cacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get cache statistics and configured cache names")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("cacheManager", cacheManager.getClass().getSimpleName());
        stats.put("cacheNames", new ArrayList<>(cacheManager.getCacheNames()));

        try {
            var props = redisConnectionFactory.getConnection().serverCommands().info("memory");
            if (props != null) {
                stats.put("redisMemoryUsed", props.getProperty("used_memory_human"));
                stats.put("redisMemoryPeak", props.getProperty("used_memory_peak_human"));
            }
        } catch (Exception e) {
            stats.put("redisStatus", "unavailable: " + e.getMessage());
        }

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Evict all entries from a specific cache")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.notFound().build();
        }
        cache.clear();
        return ResponseEntity.ok(Map.of("status", "evicted", "cache", cacheName));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Evict all caches")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        Collection<String> names = cacheManager.getCacheNames();
        names.forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        return ResponseEntity.ok(Map.of("status", "all_evicted", "caches", new ArrayList<>(names)));
    }
}
