package com.arogyalens.ai;

import com.arogyalens.config.ArogyaLensProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bounded LRU cache with a time-to-live for identical AI requests.
 * Keys are SHA-256 hashes of the request, so no prompt or document content is retained as a key.
 * Saves the (very small) Gemini free-tier quota when users retry the same scan or question.
 */
@Component
public class AiResponseCache {

    private final int maxEntries;
    private final long ttlMillis;
    private final Clock clock;
    private final Map<String, Entry> entries;

    @Autowired
    public AiResponseCache(ArogyaLensProperties properties) {
        this(properties.cache().maxEntries(), properties.cache().ttlSeconds() * 1000L, Clock.systemUTC());
    }

    AiResponseCache(int maxEntries, long ttlMillis, Clock clock) {
        this.maxEntries = Math.max(0, maxEntries);
        this.ttlMillis = Math.max(0, ttlMillis);
        this.clock = clock;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > AiResponseCache.this.maxEntries;
            }
        };
    }

    /** Returns a stable SHA-256 hex digest of the given request parts. */
    public static String key(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update((part == null ? "" : part).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public synchronized Optional<String> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (clock.millis() - entry.createdAt() > ttlMillis) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public synchronized void put(String key, String value) {
        if (maxEntries == 0 || ttlMillis == 0 || value == null) {
            return;
        }
        entries.put(key, new Entry(value, clock.millis()));
    }

    public synchronized int size() {
        return entries.size();
    }

    private record Entry(String value, long createdAt) {}
}
