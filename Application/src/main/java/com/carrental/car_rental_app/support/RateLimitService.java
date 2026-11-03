package com.carrental.car_rental_app.support;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Small in-memory sliding-window counter used to slow down password guessing and email spam. Resets when the app restarts. */
@Component
public class RateLimitService {
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** True if {@code key} already has {@code max} or more recorded hits inside the last {@code windowMs} milliseconds. */
    public boolean isBlocked(String key, int max, long windowMs) {
        Deque<Long> d = hits.get(key);
        if (d == null) return false;
        synchronized (d) {
            purge(d, windowMs);
            return d.size() >= max;
        }
    }

    public void record(String key, long windowMs) {
        Deque<Long> d = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            purge(d, windowMs);
            d.addLast(System.currentTimeMillis());
        }
        if (hits.size() > 20_000) hits.entrySet().removeIf(e -> { synchronized (e.getValue()) { purge(e.getValue(), windowMs); return e.getValue().isEmpty(); } });
    }

    public void clear(String key) { hits.remove(key); }

    private static void purge(Deque<Long> d, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        while (!d.isEmpty() && d.peekFirst() < cutoff) d.pollFirst();
    }
}
