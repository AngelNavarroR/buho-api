package org.angbyte.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class BuhoCache {

    private static final long DEFAULT_CACHE_DURATION = TimeUnit.HOURS.toDays(6);
    private final ConcurrentHashMap<String, CacheEntry> cache;

    public BuhoCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    private void init() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toDays(3));
                    cleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        if (entry != null && entry.isExpired()) {
            cache.remove(key);
        }
        return null;
    }

    public boolean containsKey(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    public void add(String key, Object value) {
        add(key, value, DEFAULT_CACHE_DURATION);
    }

    public void add(String key, Object value, long durationMillis) {
        cache.put(key, new CacheEntry(value, durationMillis));
    }

    public void clear() {
        cache.clear();
    }

    private void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class CacheEntry {
        private final Object value;
        private final long expirationTime;

        CacheEntry(Object value, long durationMillis) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + durationMillis;
        }

        Object getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}