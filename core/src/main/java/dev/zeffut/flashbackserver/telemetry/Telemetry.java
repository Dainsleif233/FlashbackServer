package dev.zeffut.flashbackserver.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class Telemetry {

    private static final Gson GSON = new Gson();
    private static final String PLACEHOLDER_KEY = "phc_REPLACE_WITH_YOUR_KEY";

    private final boolean enabled;
    private final String host;
    private final String projectKey;
    private final String distinctId;
    private final String pluginVersion;
    private final Logger logger;

    public Telemetry(boolean enabledConfig, String host, String projectKey,
                     String distinctId, String pluginVersion, Logger logger) {
        this.enabled = enabledConfig
                && projectKey != null
                && !projectKey.trim().isEmpty()
                && !projectKey.equals(PLACEHOLDER_KEY);
        this.host = host;
        this.projectKey = projectKey;
        this.distinctId = distinctId;
        this.pluginVersion = pluginVersion;
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Package-private: builds the JSON payload for a PostHog capture event.
     * No PII — distinct_id is a server-scoped random UUID.
     */
    String buildPayload(String event, Map<String, Object> props) {
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            properties.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
        }
        properties.addProperty("$lib", "flashback-server");
        properties.addProperty("plugin_version", pluginVersion);

        JsonObject payload = new JsonObject();
        payload.addProperty("api_key", projectKey);
        payload.addProperty("event", event);
        payload.addProperty("distinct_id", distinctId);
        payload.add("properties", properties);

        return GSON.toJson(payload);
    }

    /**
     * Captures an event asynchronously. Never throws — telemetry must never affect the server.
     * Uses HttpURLConnection so core stays Java 8 compatible for Paper 1.16.1.
     */
    public void capture(String event, Map<String, Object> props) {
        if (!enabled) return;
        final String payload;
        try {
            payload = buildPayload(event, props);
        } catch (Throwable t) {
            return;
        }
        final String url = host + "/i/v0/e/";
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                    conn.setFixedLengthStreamingMode(body.length);
                    OutputStream out = conn.getOutputStream();
                    try {
                        out.write(body);
                    } finally {
                        out.close();
                    }
                    conn.getResponseCode();
                } catch (Throwable ignored) {
                    // Swallow all errors: telemetry must never propagate into server threads
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }, "FlashbackServer-Telemetry");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Loads (or creates) an anonymous server UUID from {@code dataFolder/.telemetry-id}.
     * Swallows all IO errors, returning a fresh random UUID if persistence fails.
     */
    public static String loadOrCreateDistinctId(Path dataFolder) {
        Path idFile = dataFolder.resolve(".telemetry-id");
        try {
            if (Files.exists(idFile)) {
                byte[] raw = Files.readAllBytes(idFile);
                String content = new String(raw, StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
            String newId = UUID.randomUUID().toString();
            Files.createDirectories(dataFolder);
            Files.write(idFile, newId.getBytes(StandardCharsets.UTF_8));
            return newId;
        } catch (IOException e) {
            return UUID.randomUUID().toString();
        }
    }
}
