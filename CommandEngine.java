package com.iris.aivoiceassistant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CommandEngine {
    public interface Result { String reply(); boolean handled(); Intent action(); }
    private CommandEngine() {}

    public static Result handle(Context context, String raw) {
        final String input = raw == null ? "" : raw.trim();
        final String s = input.toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return result("Please say a command.", false, null);
        if (containsAny(s, "hello", "hi iris", "hey iris", "hello iris")) return result("Hello. IRIS is ready.", true, null);
        if (containsAny(s, "what time", "time now", "current time")) {
            return result("The time is " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()), true, null);
        }
        if (containsAny(s, "what date", "today's date", "today date", "what is the date")) {
            return result("Today is " + new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(new Date()), true, null);
        }
        if (s.equals("open settings") || s.contains("open phone settings")) return result("Opening settings.", true, new Intent(Settings.ACTION_SETTINGS));
        if (s.contains("open youtube")) return webIntent("https://www.youtube.com", "Opening YouTube.");
        if (s.contains("open browser") || s.contains("open chrome")) return webIntent("https://www.google.com", "Opening the browser.");
        if (s.startsWith("search ") || s.startsWith("google ") || s.startsWith("search for ")) {
            String q = input.replaceFirst("(?i)^search( for)?\\s+", "").replaceFirst("(?i)^google\\s+", "").trim();
            return webIntent("https://www.google.com/search?q=" + Uri.encode(q), "Searching for " + q + ".");
        }
        if (s.startsWith("set alarm ")) {
            String time = input.substring("set alarm ".length()).trim();
            String[] p = time.split(":");
            if (p.length == 2) {
                try {
                    int hour = Integer.parseInt(p[0]); int minute = Integer.parseInt(p[1]);
                    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, hour).putExtra(AlarmClock.EXTRA_MINUTES, minute).putExtra(AlarmClock.EXTRA_MESSAGE, "IRIS AI alarm");
                        return result("Opening the alarm setup.", true, i);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return result("Use alarm time like: set alarm 07:30", true, null);
        }
        if (s.startsWith("set timer ")) {
            String digits = s.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    long seconds = Long.parseLong(digits);
                    if (seconds > 0 && seconds <= 86400) {
                        Intent i = new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, (int) seconds).putExtra(AlarmClock.EXTRA_MESSAGE, "IRIS AI timer");
                        return result("Opening the timer setup.", true, i);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return result("Use a timer like: set timer 60 seconds.", true, null);
        }
        if (s.equals("help") || s.contains("what can you do")) return result("I can listen, answer through the configured AI service, search the web, open apps and settings, set alarms and timers, and keep local command history.", true, null);
        if (s.startsWith("open app ")) return result("I can open apps from the app launcher, but this public build needs an exact installed app name and launcher entry.", false, null);
        return result("I heard: " + input + ". This command needs the Gemini backend or another configured AI service.", false, null);
    }

    private static boolean containsAny(String s, String... words) { for (String w : words) if (s.contains(w)) return true; return false; }
    private static Result webIntent(String url, String reply) { return result(reply, true, new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
    private static Result result(String reply, boolean handled, Intent action) { return new SimpleResult(reply, handled, action); }
    private static final class SimpleResult implements Result {
        private final String reply; private final boolean handled; private final Intent action;
        SimpleResult(String reply, boolean handled, Intent action) { this.reply = reply; this.handled = handled; this.action = action; }
        public String reply() { return reply; } public boolean handled() { return handled; } public Intent action() { return action; }
    }
}
