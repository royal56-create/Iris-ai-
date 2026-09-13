package com.iris.aivoiceassistant;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GeminiClient {
    public interface Callback { void onSuccess(String text); void onError(String message); }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private GeminiClient() {}
    private static final GeminiClient INSTANCE = new GeminiClient();
    public static GeminiClient get() { return INSTANCE; }

    public void ask(String endpoint, String text, Callback callback) {
        if (endpoint == null || endpoint.trim().isEmpty()) { callback.onError("No AI backend is configured."); return; }
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setConnectTimeout(10000); conn.setReadTimeout(20000);
                conn.setDoOutput(true); conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                String body = "{\"message\":\"" + jsonEscape(text) + "\"}";
                conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) throw new IOException("Backend HTTP " + code);
                java.io.InputStream in = conn.getInputStream();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                String response = new String(out.toByteArray(), StandardCharsets.UTF_8);
                main.post(() -> callback.onSuccess(extractText(response)));
            } catch (Exception e) { main.post(() -> callback.onError(e.getMessage() == null ? "AI request failed." : e.getMessage())); }
            finally { if (conn != null) conn.disconnect(); }
        });
    }
    private static String jsonEscape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"); }
    private static String extractText(String raw) {
        String key = "\"text\""; int i = raw.indexOf(key); if (i < 0) return raw;
        int colon = raw.indexOf(':', i + key.length()); if (colon < 0) return raw;
        int start = raw.indexOf('"', colon + 1); if (start < 0) return raw;
        StringBuilder b = new StringBuilder(); boolean esc = false;
        for (int p = start + 1; p < raw.length(); p++) { char ch = raw.charAt(p); if (esc) { b.append(ch == 'n' ? '\n' : ch); esc = false; } else if (ch == '\\') esc = true; else if (ch == '"') break; else b.append(ch); }
        return b.toString();
    }
}
