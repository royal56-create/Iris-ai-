package com.iris.aivoiceassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_AUDIO = 100;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView transcript, status, historyText;
    private Button micButton;
    private boolean listening = false;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("iris_settings", MODE_PRIVATE);
        tts = new TextToSpeech(this, this);
        buildUi();
        prepareRecognizer();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14), dp(12), dp(14), dp(12)); root.setBackgroundColor(Color.rgb(7,7,11));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this); logo.setImageResource(com.iris.aivoiceassistant.R.drawable.iris_logo); logo.setScaleType(ImageView.ScaleType.CENTER_CROP); header.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(10),0,0,0);
        TextView title = text("IRIS AI", 25, Color.WHITE, true); TextView sub = text("VOICE ASSISTANT • ANDROID", 12, Color.LTGRAY, false); titles.addView(title); titles.addView(sub); header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = button("⚙", 0xFF6C5CE7); settings.setOnClickListener(v -> showSettings()); header.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));

        ImageView heroLogo = new ImageView(this); heroLogo.setImageResource(R.drawable.iris_logo); heroLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE); root.addView(heroLogo, new LinearLayout.LayoutParams(-1, dp(190)));
        TextView tagline = text("JUST SPEAK… IRIS HANDLES EVERYTHING", 14, 0xFF00E5FF, true); tagline.setGravity(Gravity.CENTER); root.addView(tagline);
        status = text("READY • Tap the microphone", 13, 0xFFB0B4C0, false); status.setGravity(Gravity.CENTER); root.addView(status, new LinearLayout.LayoutParams(-1, dp(36)));

        transcript = text("Your command will appear here.", 17, Color.WHITE, false); transcript.setGravity(Gravity.CENTER); transcript.setPadding(dp(16), dp(14), dp(16), dp(14)); root.addView(card(transcript, 0xFF151520), new LinearLayout.LayoutParams(-1, dp(96)));
        micButton = button("🎙  TAP TO SPEAK", 0xFF00BFA5); micButton.setTextSize(17); micButton.setOnClickListener(v -> toggleListening()); root.addView(micButton, new LinearLayout.LayoutParams(-1, dp(62)));

        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER); actions.setPadding(0, dp(10),0,dp(10));
        Button history = button("HISTORY", 0xFF2979FF); history.setOnClickListener(v -> showHistory());
        Button overlay = button("FLOATING", 0xFFFF6D00); overlay.setOnClickListener(v -> toggleOverlay());
        Button help = button("HELP", 0xFFAB47BC); help.setOnClickListener(v -> speakAndShow("I can listen, use the configured AI backend, search the web, open apps and settings, set alarms and timers, and keep local history."));
        actions.addView(history, weightParams(1)); actions.addView(overlay, weightParams(1)); actions.addView(help, weightParams(1)); root.addView(actions);

        ScrollView scroll = new ScrollView(this); historyText = text("QUICK COMMANDS\n• What time is it\n• Search for space news\n• Open YouTube\n• Open settings\n• Set alarm 07:30\n• Set timer 60 seconds\n• What can you do", 14, 0xFFD6D8E0, false); historyText.setPadding(dp(16),dp(14),dp(16),dp(14)); scroll.addView(card(historyText, 0xFF101018)); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void prepareRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { status.setText("Speech recognition is unavailable on this device"); return; }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { status.setText("LISTENING…"); }
            public void onBeginningOfSpeech() { status.setText("HEARING YOUR VOICE…"); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { listening = false; micButton.setText("🎙  TAP TO SPEAK"); status.setText("PROCESSING…"); }
            public void onError(int e) { listening = false; micButton.setText("🎙  TAP TO SPEAK"); status.setText("READY • Speech error " + e); }
            public void onResults(Bundle b) { handleResults(b); }
            public void onPartialResults(Bundle b) {}
            public void onEvent(int t, Bundle b) {}
        });
    }

    private void toggleListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO); return; }
        if (recognizer == null) { prepareRecognizer(); if (recognizer == null) return; }
        if (listening) { recognizer.stopListening(); listening = false; micButton.setText("🎙  TAP TO SPEAK"); status.setText("READY"); return; }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        listening = true; micButton.setText("■  STOP LISTENING"); recognizer.startListening(i);
    }

    private void handleResults(Bundle b) {
        ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if (r == null || r.isEmpty()) { status.setText("READY"); return; }
        String command = r.get(0); transcript.setText(command); runCommand(command);
    }

    private void runCommand(String command) {
        if (command != null && command.toLowerCase(Locale.ROOT).startsWith("open app ")) {
            String name = command.substring(9).trim().toLowerCase(Locale.ROOT);
            Intent launch = findLauncherApp(name);
            if (launch != null) {
                String reply = "Opening " + command.substring(9).trim() + ".";
                HistoryStore.add(this, command, reply);
                speakAndShow(reply);
                try { startActivity(launch); } catch (Exception ignored) {}
                return;
            }
        }
        CommandEngine.Result result = CommandEngine.handle(this, command);
        if (result.action() != null) {
            HistoryStore.add(this, command, result.reply()); speakAndShow(result.reply()); try { startActivity(result.action()); } catch (Exception e) { speakAndShow("I could not open that action on this device."); } return; }
        if (result.handled()) { HistoryStore.add(this, command, result.reply()); speakAndShow(result.reply()); return; }
        String endpoint = prefs.getString("backend_url", "");
        if (endpoint.isEmpty()) { HistoryStore.add(this, command, result.reply()); speakAndShow(result.reply()); return; }
        status.setText("AI BACKEND • THINKING…");
        GeminiClient.get().ask(endpoint, command, new GeminiClient.Callback() {
            public void onSuccess(String text) { transcript.setText(text); HistoryStore.add(MainActivity.this, command, text); speakAndShow(text); }
            public void onError(String msg) { speakAndShow("AI service is unavailable. " + msg); }
        });
    }


    private Intent findLauncherApp(String wanted) {
        android.content.pm.PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null); query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        for (android.content.pm.ResolveInfo info : apps) {
            String label = String.valueOf(info.loadLabel(pm)).toLowerCase(Locale.ROOT);
            if (label.equals(wanted) || label.contains(wanted)) {
                Intent launch = new Intent(Intent.ACTION_MAIN); launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setClassName(info.activityInfo.packageName, info.activityInfo.name); return launch;
            }
        }
        return null;
    }

    private void speakAndShow(String s) { status.setText("IRIS • READY"); transcript.setText(s); if (tts != null) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "iris"); }

    private void showHistory() {
        List<String> rows = HistoryStore.all(this); StringBuilder b = new StringBuilder("COMMAND HISTORY\n\n"); if (rows.isEmpty()) b.append("No commands yet."); else for (String row: rows) b.append("• ").append(row).append("\n\n");
        new android.app.AlertDialog.Builder(this).setTitle("IRIS HISTORY").setMessage(b.toString()).setPositiveButton("Close", null).setNegativeButton("Clear", (d,w)->{HistoryStore.clear(this);}).show();
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(4),dp(20),0);
        EditText endpoint = new EditText(this); endpoint.setHint("https://your-secure-backend.example/api/chat"); endpoint.setSingleLine(true); endpoint.setText(prefs.getString("backend_url", "")); box.addView(endpoint, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView note = text("Gemini key stays on your secure backend. The APK stores only the HTTPS endpoint. Clear the field to disable cloud AI.", 12, 0xFFB8BAC5, false); box.addView(note);
        new android.app.AlertDialog.Builder(this).setTitle("IRIS AI SETTINGS").setView(box).setPositiveButton("Save", (d,w)->{String u=endpoint.getText().toString().trim(); if (!u.isEmpty() && !u.startsWith("https://")) {Toast.makeText(this,"Use an HTTPS backend URL.",Toast.LENGTH_LONG).show(); return;} prefs.edit().putString("backend_url",u).apply();}).setNeutralButton("Overlay Permission", (d,w)->openOverlaySettings()).setNegativeButton("Cancel", null).show();
    }

    private void toggleOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { openOverlaySettings(); return; }
        try { startService(new Intent(this, AssistantOverlayService.class)); Toast.makeText(this,"Floating IRIS enabled for this session.",Toast.LENGTH_SHORT).show(); } catch (Exception e) { Toast.makeText(this,"Could not start floating assistant.",Toast.LENGTH_LONG).show(); }
    }
    private void openOverlaySettings() { try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); } }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) { super.onRequestPermissionsResult(request,permissions,results); if (request==REQ_AUDIO && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) toggleListening(); else if (request==REQ_AUDIO) Toast.makeText(this,"Microphone permission is required for voice input.",Toast.LENGTH_LONG).show(); }
    @Override public void onInit(int statusCode) { if (tts != null && statusCode == TextToSpeech.SUCCESS) tts.setLanguage(Locale.getDefault()); }
    @Override protected void onDestroy() { if (recognizer != null) { recognizer.destroy(); recognizer=null; } if (tts != null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }

    private TextView text(String s, int size, int color, boolean bold) { TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT, bold?Typeface.BOLD:Typeface.NORMAL); return v; }
    private Button button(String s, int color) { Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(12); b.setAllCaps(false); b.setBackground(round(color, dp(14))); b.setPadding(dp(4),0,dp(4),0); return b; }
    private View card(View v, int color) { v.setBackground(round(color,dp(18))); return v; }
    private GradientDrawable round(int color, float radius) { GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); g.setStroke(dp(1),0xFF303346); return g; }
    private LinearLayout.LayoutParams weightParams(int w) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(50),w); p.setMargins(dp(4),0,dp(4),0); return p; }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
}
