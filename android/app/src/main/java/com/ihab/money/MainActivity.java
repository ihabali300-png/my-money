package com.ihab.money;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/** Full-screen WebView around «فلوسي» (served from GitHub Pages, so it updates by itself) + small native helpers. */
public class MainActivity extends Activity {
    private static final String HOME = "https://ihabali300-png.github.io/my-money/";
    private static final int PICK = 41, VOICE = 43, SMS_PERM = 44;
    private WebView web;
    private ValueCallback<Uri[]> pending;
    private Uri cameraUri;

    private void js(String code) { runOnUiThread(() -> web.evaluateJavascript(code, null)); }

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " IHABMoneyApk");
        web.addJavascriptInterface(new Object() {
            // 🔔 notifications
            @JavascriptInterface public void show(String title, String body) { Notify.show(MainActivity.this, (int) (System.currentTimeMillis() % 100000) + 10, title, body); }
            @JavascriptInterface public void setDaily(boolean on, int h, int m, String title, String body) {
                SharedPreferences.Editor e = getSharedPreferences(Notify.PREF, 0).edit();
                e.putBoolean("on", on).putInt("h", h).putInt("m", m).putString("title", title).putString("body", body).apply();
                Notify.schedule(MainActivity.this);
            }
            @JavascriptInterface public void askPermission() {
                if (Build.VERSION.SDK_INT >= 33) runOnUiThread(() -> requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 51));
            }
            // 📩 bank SMS
            @JavascriptInterface public boolean hasSms() { return checkSelfPermission("android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED; }
            @JavascriptInterface public void askSms() { runOnUiThread(() -> requestPermissions(new String[]{"android.permission.READ_SMS", "android.permission.RECEIVE_SMS"}, SMS_PERM)); }
            @JavascriptInterface public String readSms(String since) {
                JSONArray out = new JSONArray();
                if (checkSelfPermission("android.permission.READ_SMS") != PackageManager.PERMISSION_GRANTED) return out.toString();
                try (Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"_id", "address", "body", "date"},
                        "date > ?", new String[]{since}, "date DESC")) {
                    int n = 0;
                    while (c != null && c.moveToNext() && n < 300) {
                        JSONObject o = new JSONObject();
                        o.put("id", c.getString(0)); o.put("from", c.getString(1)); o.put("body", c.getString(2)); o.put("date", c.getLong(3));
                        out.put(o); n++;
                    }
                } catch (Exception e) { }
                return out.toString();
            }
            // 🎙️ voice
            @JavascriptInterface public void listen() {
                runOnUiThread(() -> {
                    Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
                    i.putExtra(RecognizerIntent.EXTRA_PROMPT, "قول الصنف والمبلغ… مثلاً: برجر هرفي خمسة وعشرين");
                    i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                    try { startActivityForResult(i, VOICE); } catch (ActivityNotFoundException e) { js("window.onVoice&&window.onVoice(null)"); }
                });
            }
        }, "IHABN");
        Notify.channel(this);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("ihabali300-png.github.io".equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException e) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pending != null) pending.onReceiveValue(null);
                pending = cb;
                Intent intent;
                if (p.isCaptureEnabled()) {          // <input capture> → camera
                    try {
                        File dir = new File(getCacheDir(), "photos"); dir.mkdirs();
                        File f = new File(dir, "shot_" + System.currentTimeMillis() + ".jpg");
                        cameraUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) { cameraUri = null; intent = p.createIntent(); }
                } else {                              // gallery
                    cameraUri = null;
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                }
                try { startActivityForResult(intent, PICK); }
                catch (ActivityNotFoundException e) { pending.onReceiveValue(null); pending = null; return false; }
                return true;
            }
        });
        if (saved != null) web.restoreState(saved); else web.loadUrl(HOME + "?apk=1");
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == VOICE) {
            String txt = null;
            if (res == RESULT_OK && data != null) {
                ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (r != null && !r.isEmpty()) txt = new JSONArray(r).toString();
            }
            js("window.onVoice&&window.onVoice(" + (txt == null ? "null" : txt) + ")");
            return;
        }
        if (req != PICK || pending == null) { super.onActivityResult(req, res, data); return; }
        Uri[] out = null;
        if (res == RESULT_OK) {
            if (cameraUri != null) out = new Uri[]{ cameraUri };
            else if (data != null && data.getData() != null) out = new Uri[]{ data.getData() };
        }
        pending.onReceiveValue(out); pending = null;
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == SMS_PERM) js("window.onSmsPerm&&window.onSmsPerm(" + (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) + ")");
    }

    @Override protected void onResume() { super.onResume(); if (web != null) js("window.onAppResume&&window.onAppResume()"); }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
