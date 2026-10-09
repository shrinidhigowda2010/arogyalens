package com.arogyalens.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private long now = 0;
    private final Clock clock =
            new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return Instant.ofEpochMilli(now);
                }
            };

    @Test
    void allowsBurstUpToCapacityThenRefills() {
        RateLimitFilter filter =
                new RateLimitFilter(3, new ObjectMapper().findAndRegisterModules(), clock);
        assertThat(filter.tryConsume("1.1.1.1")).isTrue();
        assertThat(filter.tryConsume("1.1.1.1")).isTrue();
        assertThat(filter.tryConsume("1.1.1.1")).isTrue();
        assertThat(filter.tryConsume("1.1.1.1")).isFalse();
        assertThat(filter.tryConsume("2.2.2.2")).isTrue();
        now += 20_000; // one token per 20s at 3/min
        assertThat(filter.tryConsume("1.1.1.1")).isTrue();
    }

    @Test
    void returns429JsonWhenLimited() throws Exception {
        RateLimitFilter filter =
                new RateLimitFilter(1, new ObjectMapper().findAndRegisterModules(), clock);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/voice/query");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("60");
        assertThat(res.getContentAsString()).contains("RATE_LIMITED");
    }

    @Test
    void ignoresReadOnlyRequests() throws Exception {
        RateLimitFilter filter =
                new RateLimitFilter(1, new ObjectMapper().findAndRegisterModules(), clock);
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(
                    new MockHttpServletRequest("GET", "/api/health"), res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }
}
