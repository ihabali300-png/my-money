package com.ihab.money;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Full-screen WebView around «فلوسي» (served from GitHub Pages, so it updates by itself). */
public class MainActivity extends Activity {
    private static final String HOME = "https://ihabali300-png.github.io/my-money/";
    private WebView web;

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
        // 🔔 bridge: the page sends notification text; Notify shows it / schedules the daily reminder
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void show(String title, String body) { Notify.show(MainActivity.this, (int) (System.currentTimeMillis() % 100000) + 10, title, body); }
            @JavascriptInterface public void setDaily(boolean on, int h, int m, String title, String body) {
                SharedPreferences.Editor e = getSharedPreferences(Notify.PREF, 0).edit();
                e.putBoolean("on", on).putInt("h", h).putInt("m", m).putString("title", title).putString("body", body).apply();
                Notify.schedule(MainActivity.this);
            }
            @JavascriptInterface public void askPermission() {
                if (Build.VERSION.SDK_INT >= 33) runOnUiThread(() -> requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 51));
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
        if (saved != null) web.restoreState(saved); else web.loadUrl(HOME + "?apk=1");
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
