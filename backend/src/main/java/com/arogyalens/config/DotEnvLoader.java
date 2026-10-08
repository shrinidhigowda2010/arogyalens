package com.arogyalens.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads KEY=VALUE pairs from a local .env into system properties
 * before Spring Boot starts, so ${GEMINI_API_KEY} resolves correctly.
 */
public final class DotEnvLoader {

    private DotEnvLoader() {}

    public static void load() {
        Path loadedFrom = null;
        Map<String, String> values = new LinkedHashMap<>();

        for (Path path : candidatePaths()) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                List<String> lines = Files.readAllLines(path);
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
                loadedFrom = path;
                break;
            } catch (IOException ignored) {
                // try next candidate
            }
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // Prefer real process env; otherwise apply .env (including overwriting blank props).
            if (System.getenv(key) != null) {
                continue;
            }
            String existing = System.getProperty(key);
            if (existing == null || existing.isBlank()) {
                System.setProperty(key, value == null ? "" : value);
            }
        }

        boolean gemini = hasGeminiKey();
        if (loadedFrom != null) {
            System.out.println("ArogyaLens: loaded env from " + loadedFrom.toAbsolutePath()
                    + " | GEMINI_API_KEY=" + (gemini ? "configured" : "missing"));
        } else {
            System.out.println("ArogyaLens: no .env file found | GEMINI_API_KEY="
                    + (gemini ? "configured" : "missing"));
        }
    }

    public static boolean hasGeminiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) {
            return true;
        }
        String prop = System.getProperty("GEMINI_API_KEY");
        return prop != null && !prop.isBlank();
    }

    private static List<Path> candidatePaths() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path userDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        return List.of(
                cwd.resolve(".env"),
                cwd.resolve("../.env").normalize(),
                userDir.resolve(".env"),
                userDir.resolve("../.env").normalize(),
                cwd.getParent() != null ? cwd.getParent().resolve(".env") : cwd.resolve(".env")
        );
    }
}
