package com.arogyalens.service;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.exception.ArogyaLensException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final ArogyaLensProperties properties;

    public SessionService(ArogyaLensProperties properties) {
        this.properties = properties;
    }

    public String createId() {
        return UUID.randomUUID().toString();
    }

    public void save(String sessionId, AnalysisResponse response, String contextText) {
        sessions.put(sessionId, new SessionRecord(response, contextText, Instant.now()));
        cleanup();
    }

    public AnalysisResponse require(String sessionId) {
        return get(sessionId).orElseThrow(() -> new ArogyaLensException(
                "SESSION_NOT_FOUND",
                "Session missing",
                "This session is no longer available. Please upload the document again."
        ));
    }

    public Optional<AnalysisResponse> get(String sessionId) {
        cleanup();
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        SessionRecord record = sessions.get(sessionId);
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(record.response());
    }

    public String context(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }
        SessionRecord record = sessions.get(sessionId);
        return record == null ? "" : record.contextText();
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(properties.session().ttlMinutes() * 60L);
        sessions.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }

    private record SessionRecord(AnalysisResponse response, String contextText, Instant createdAt) {}
}
