package com.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class EnvUtils {

    private EnvUtils() {
    }

    /**
     * Returns the value of the given environment variable, falling back to a local .env file if present.
     */
    public static Optional<String> get(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return Optional.of(value);
        }

        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return Optional.empty();
        }

        try (BufferedReader reader = Files.newBufferedReader(envFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String name = line.substring(0, idx).trim();
                if (!name.equals(key)) {
                    continue;
                }
                String raw = line.substring(idx + 1).trim();
                //strip optional surrounding quotes
                if ((raw.startsWith("\"") && raw.endsWith("\"")) ||
                        (raw.startsWith("'") && raw.endsWith("'"))) {
                    raw = raw.substring(1, raw.length() - 1);
                }
                return Optional.of(raw);
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }
}
