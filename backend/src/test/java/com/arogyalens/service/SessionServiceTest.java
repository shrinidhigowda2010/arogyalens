package com.arogyalens.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.support.TestProps;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SessionServiceTest {

    private Instant now = Instant.parse("2026-01-01T00:00:00Z");
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
                    return now;
                }
            };
    private final SessionService sessions =
            new SessionService(TestProps.defaults(), new PrivacyService(), clock);

    @Test
    void blankOrNullIdsAreSafe() {
        assertThat(sessions.get(null)).isEmpty();
        assertThat(sessions.get("  ")).isEmpty();
        assertThat(sessions.context(null)).isEmpty();
    }

    @Test
    void contextIsPiiMaskedBeforeStorage() {
        sessions.save("s1", null, "Patient Name: Anita Sharma, phone 9876543210, HbA1c 7.2");
        assertThat(sessions.context("s1"))
                .doesNotContain("9876543210")
                .doesNotContain("Anita Sharma")
                .contains("HbA1c 7.2");
    }

    @Test
    void sessionsExpireAfterTtl() {
        sessions.save("s1", null, "ctx");
        now = now.plusSeconds(61 * 60);
        assertThat(sessions.get("s1")).isEmpty();
    }

    @Test
    void requireThrowsNotFoundForUnknownSession() {
        assertThatThrownBy(() -> sessions.require("missing"))
                .isInstanceOf(ArogyaLensException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void storeIsBounded() {
        for (int i = 0; i < SessionService.MAX_SESSIONS + 5; i++) {
            now = now.plusMillis(1);
            sessions.save("s" + i, null, "ctx");
        }
        assertThat(sessions.size()).isEqualTo(SessionService.MAX_SESSIONS);
        assertThat(sessions.context("s0")).isEmpty();
    }
}
