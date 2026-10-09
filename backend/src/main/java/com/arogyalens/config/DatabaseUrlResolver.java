package com.arogyalens.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Translates a Heroku/Render style {@code DATABASE_URL} ({@code postgres://user:pass@host:5432/db})
 * into Spring datasource properties. TLS is enforced with {@code sslmode=require} unless {@code
 * DATABASE_SSLMODE} overrides it. Without {@code DATABASE_URL} the application keeps its in-memory
 * H2 default.
 */
public final class DatabaseUrlResolver {

    private DatabaseUrlResolver() {}

    /** JDBC connection settings derived from a database URL. */
    public record JdbcSettings(String url, String username, String password) {}

    /**
     * Reads DATABASE_URL (system property or environment) and publishes spring.datasource.*
     * properties.
     */
    public static void apply() {
        String raw =
                firstNonBlank(System.getProperty("DATABASE_URL"), System.getenv("DATABASE_URL"));
        String sslMode =
                firstNonBlank(
                        System.getProperty("DATABASE_SSLMODE"), System.getenv("DATABASE_SSLMODE"));
        toJdbc(raw, sslMode)
                .ifPresent(
                        settings -> {
                            System.setProperty("spring.datasource.url", settings.url());
                            if (settings.username() != null) {
                                System.setProperty(
                                        "spring.datasource.username", settings.username());
                            }
                            if (settings.password() != null) {
                                System.setProperty(
                                        "spring.datasource.password", settings.password());
                            }
                        });
    }

    /**
     * Converts {@code postgres://} / {@code postgresql://} URLs (and passes through {@code jdbc:}
     * URLs).
     *
     * @param databaseUrl raw URL, may be null
     * @param sslMode libpq sslmode, defaults to {@code require}
     * @return settings, or empty when no usable URL is given
     */
    public static Optional<JdbcSettings> toJdbc(String databaseUrl, String sslMode) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return Optional.empty();
        }
        String url = databaseUrl.strip();
        if (url.startsWith("jdbc:")) {
            return Optional.of(new JdbcSettings(url, null, null));
        }
        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) {
            throw new IllegalArgumentException(
                    "DATABASE_URL must start with postgres://, postgresql:// or jdbc:");
        }
        URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
        String user = null;
        String password = null;
        if (uri.getRawUserInfo() != null) {
            String[] parts = uri.getRawUserInfo().split(":", 2);
            user = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : null;
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String mode = sslMode == null || sslMode.isBlank() ? "require" : sslMode.strip();
        String query = uri.getRawQuery();
        StringBuilder jdbc =
                new StringBuilder("jdbc:postgresql://")
                        .append(uri.getHost())
                        .append(':')
                        .append(port)
                        .append(uri.getRawPath());
        jdbc.append('?');
        if (query != null && !query.isBlank()) {
            jdbc.append(query).append('&');
        }
        if (query == null || !query.contains("sslmode=")) {
            jdbc.append("sslmode=").append(mode);
        } else {
            jdbc.setLength(jdbc.length() - 1);
        }
        return Optional.of(new JdbcSettings(jdbc.toString(), user, password));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b != null && !b.isBlank() ? b : null;
    }
}
