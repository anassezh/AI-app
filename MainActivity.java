package com.example.freeaichat;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final String MODEL = "gemini-3.6-flash";
    private static final String PREFS = "free_ai_chat";
    private static final String KEY_API = "api_key";
    private LinearLayout messages;
    private EditText input;
    private Button sendButton;
    private SharedPreferences prefs;
    private final ArrayList<ChatMessage> history = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        messages = findViewById(R.id.messages);
        input = findViewById(R.id.input);
        sendButton = findViewById(R.id.sendButton);
        findViewById(R.id.settingsButton).setOnClickListener(v -> showSettings());
        sendButton.setOnClickListener(v -> sendMessage());
        loadHistory();
        if (history.isEmpty()) addBubble("Hi! I'm your free AI assistant.\n\nOpen Settings and add your Gemini API key to start chatting.", false);
    }

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        String apiKey = prefs.getString(KEY_API, "").trim();
        if (apiKey.isEmpty()) { showSettings(); return; }
        addBubble(text, true); history.add(new ChatMessage("user", text)); saveHistory();
        input.setText("");
        setSending(true);
        new Thread(() -> {
            try {
                String reply = callGemini(apiKey);
                runOnUiThread(() -> { addBubble(reply, false); history.add(new ChatMessage("model", reply)); saveHistory(); setSending(false); });
            } catch (Exception e) {
                String msg = "Error: " + e.getMessage();
                runOnUiThread(() -> { addBubble(msg, false); setSending(false); });
            }
        }).start();
    }

    private String callGemini(String apiKey) throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("x-goog-api-key", apiKey);
        JSONArray contents = new JSONArray();
        for (ChatMessage m : history) {
            JSONObject item = new JSONObject(); item.put("role", m.role);
            JSONArray parts = new JSONArray(); JSONObject p = new JSONObject(); p.put("text", m.text); parts.put(p); item.put("parts", parts); contents.put(item);
        }
        JSONObject body = new JSONObject(); body.put("contents", contents);
        JSONObject generation = new JSONObject(); generation.put("temperature", 0.7); generation.put("maxOutputTokens", 1500); body.put("generationConfig", generation);
        try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String response = readAll(stream);
        if (code < 200 || code >= 300) throw new Exception("API " + code + ": " + extractError(response));
        JSONObject root = new JSONObject(response);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) throw new Exception("The AI returned no response.");
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) throw new Exception("The AI returned an empty response.");
        return parts.getJSONObject(0).optString("text", "No text returned.");
    }

    private String extractError(String json) {
        try { return new JSONObject(json).optJSONObject("error").optString("message", json); } catch (Exception e) { return json; }
    }
    private String readAll(InputStream is) throws IOException { if (is == null) return ""; BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)); StringBuilder s = new StringBuilder(); String line; while ((line=r.readLine())!=null) s.append(line); return s.toString(); }

    private void addBubble(String text, boolean user) {
        TextView bubble = new TextView(this); bubble.setText(text); bubble.setTextSize(16); bubble.setTextColor(user ? Color.WHITE : Color.rgb(35,32,38)); bubble.setPadding(18,14,18,14); bubble.setBackgroundResource(user ? R.drawable.bubble_user : R.drawable.bubble_ai);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.min((int)(getResources().getDisplayMetrics().widthPixels*0.86f), 900), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = user ? Gravity.END : Gravity.START; lp.setMargins(6,6,6,6); messages.addView(bubble, lp);
        messages.post(() -> ((ScrollView) findViewById(R.id.scroll)).fullScroll(View.FOCUS_DOWN));
    }

    private void setSending(boolean sending) {
        sendButton.setEnabled(!sending); input.setEnabled(!sending); sendButton.setText(sending ? "…" : "Send");
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(36,8,36,0);
        TextView help = new TextView(this); help.setText("Create a Gemini API key in Google AI Studio, then paste it here. The key is stored only on this phone.\n\nThis app uses Gemini's free developer tier; usage limits still apply."); help.setTextSize(15); box.addView(help);
        EditText key = new EditText(this); key.setHint("AIza…"); key.setSingleLine(true); key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); key.setText(prefs.getString(KEY_API, "")); box.addView(key);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("AI Settings").setView(box).setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { String k=key.getText().toString().trim(); if(k.isEmpty()) { key.setError("Enter an API key"); return; } prefs.edit().putString(KEY_API,k).apply(); Toast.makeText(this,"API key saved",Toast.LENGTH_SHORT).show(); dialog.dismiss(); }));
        dialog.show();
    }

    private void saveHistory() {
        JSONArray a = new JSONArray(); try { for (ChatMessage m: history) { JSONObject o=new JSONObject(); o.put("role",m.role); o.put("text",m.text); a.put(o); } prefs.edit().putString("history",a.toString()).apply(); } catch(Exception ignored){}
    }
    private void loadHistory() {
        String raw=prefs.getString("history",""); if(raw.isEmpty()) return; try { JSONArray a=new JSONArray(raw); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i); ChatMessage m=new ChatMessage(o.getString("role"),o.getString("text")); history.add(m); addBubble(m.text,m.role.equals("user"));} } catch(Exception ignored){}
    }

    static class ChatMessage { final String role, text; ChatMessage(String r,String t){role=r;text=t;} }
}
