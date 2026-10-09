package com.arogyalens.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One PII-masked history item (a scan summary or a question and answer). */
@Entity
@Table(name = "history_entry")
public class HistoryEntry {

    /** What produced the entry. */
    public enum Kind {
        SCAN,
        CHAT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Kind kind;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HistoryEntry() {}

    public HistoryEntry(
            String deviceId,
            Kind kind,
            String title,
            String summary,
            String language,
            Instant createdAt) {
        this.deviceId = deviceId;
        this.kind = kind;
        this.title = title;
        this.summary = summary;
        this.language = language;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Kind getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getLanguage() {
        return language;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
