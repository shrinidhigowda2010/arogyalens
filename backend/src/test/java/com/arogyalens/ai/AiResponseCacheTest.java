package com.arogyalens.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AiResponseCacheTest {

    private final AtomicLong now = new AtomicLong(0);
    private final Clock clock =
            new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return Instant.ofEpochMilli(now.get());
                }
            };

    @Test
    void expiresEntriesAfterTtl() {
        AiResponseCache cache = new AiResponseCache(10, 1_000, clock);
        cache.put("k", "v");
        assertThat(cache.get("k")).contains("v");
        now.set(1_001);
        assertThat(cache.get("k")).isEmpty();
    }

    @Test
    void evictsLeastRecentlyUsedBeyondCapacity() {
        AiResponseCache cache = new AiResponseCache(2, 60_000, clock);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a");
        cache.put("c", "3");
        assertThat(cache.get("b")).isEmpty();
        assertThat(cache.get("a")).contains("1");
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void keyIsStableHashThatHidesContent() {
        String key = AiResponseCache.key("text", "my phone is 9876543210");
        assertThat(key).hasSize(64).doesNotContain("9876543210");
        assertThat(AiResponseCache.key("text", "my phone is 9876543210")).isEqualTo(key);
        assertThat(AiResponseCache.key("a", "bc")).isNotEqualTo(AiResponseCache.key("ab", "c"));
    }
}
