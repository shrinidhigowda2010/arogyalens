package com.arogyalens.service;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.privacy.PrivacyService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Short-lived, in-memory store of analysis sessions used to ground follow-up questions. Context
 * text is PII-masked before it is stored, entries expire after the configured TTL, and the store is
 * bounded so it cannot grow without limit.
 */
@Service
public class SessionService {

    static final int MAX_SESSIONS = 500;

    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final ArogyaLensProperties properties;
    private final PrivacyService privacyService;
    private final Clock clock;

    @Autowired
    public SessionService(ArogyaLensProperties properties, PrivacyService privacyService) {
        this(properties, privacyService, Clock.systemUTC());
    }

    SessionService(ArogyaLensProperties properties, PrivacyService privacyService, Clock clock) {
        this.properties = properties;
        this.privacyService = privacyService;
        this.clock = clock;
    }

    /**
     * @return a new random session identifier
     */
    public String createId() {
        return UUID.randomUUID().toString();
    }

    /** Stores a result and its grounding context (PII-masked) under the given id. */
    public void save(String sessionId, AnalysisResponse response, String contextText) {
        cleanup();
        String masked =
                privacyService.scanAndRedact(contextText == null ? "" : contextText).redactedText();
        sessions.put(sessionId, new SessionRecord(response, masked, clock.instant()));
        evictOverflow();
    }

    /**
     * @throws ArogyaLensException SESSION_NOT_FOUND when the session is missing or expired
     */
    public AnalysisResponse require(String sessionId) {
        return get(sessionId)
                .orElseThrow(
                        () ->
                                new ArogyaLensException(
                                        "SESSION_NOT_FOUND",
                                        "Session missing",
                                        "This session is no longer available. Please upload the document again.",
                                        HttpStatus.NOT_FOUND));
    }

    /**
     * @return the analysis for a session id, or empty if the id is blank, unknown or expired
     */
    public Optional<AnalysisResponse> get(String sessionId) {
        cleanup();
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId)).map(SessionRecord::response);
    }

    /**
     * @return the PII-masked grounding context for a session, or an empty string
     */
    public String context(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }
        SessionRecord record = sessions.get(sessionId);
        return record == null ? "" : record.contextText();
    }

    int size() {
        return sessions.size();
    }

    private void cleanup() {
        Instant cutoff = clock.instant().minusSeconds(properties.session().ttlMinutes() * 60L);
        sessions.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }

    private void evictOverflow() {
        while (sessions.size() > MAX_SESSIONS) {
            sessions.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().createdAt()))
                    .ifPresent(e -> sessions.remove(e.getKey()));
        }
    }

    private record SessionRecord(
            AnalysisResponse response, String contextText, Instant createdAt) {}
}
