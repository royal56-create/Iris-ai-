package com.iris.aivoiceassistant;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class HistoryStore {
    private static final String PREF = "iris_memory";
    private static final String KEY = "history";
    private static final int MAX = 50;
    private HistoryStore() {}
    public static void add(Context c, String user, String reply) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String old = p.getString(KEY, "");
        String line = clean(user) + " | " + clean(reply);
        String next = line + (old.isEmpty() ? "" : "\n" + old);
        String[] rows = next.split("\\n");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < rows.length && i < MAX; i++) { if (i > 0) b.append('\n'); b.append(rows[i]); }
        p.edit().putString(KEY, b.toString()).apply();
    }
    public static List<String> all(Context c) {
        String raw = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "");
        List<String> out = new ArrayList<>(); if (!raw.isEmpty()) for (String s : raw.split("\\n")) out.add(s); return out;
    }
    public static void clear(Context c) { c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply(); }
    private static String clean(String s) { return s == null ? "" : s.replace("\\n", " ").replace("|", "/"); }
}
