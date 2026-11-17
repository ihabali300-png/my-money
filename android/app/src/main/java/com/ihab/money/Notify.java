package com.ihab.money;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

/** Daily reminder + instant notifications for «فلوسي». The page sends the text; this only shows it. */
public class Notify extends BroadcastReceiver {
    static final String CH = "felousy";
    static final String PREF = "felousy_notif";

    static void channel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "فلوسي", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("تذكير المصاريف ونصايح الميزانية");
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    static void show(Context c, int id, String title, String body) {
        channel(c);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, CH) : new android.app.Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(body)
         .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
         .setColor(0xFFD4AF37).setAutoCancel(true).setContentIntent(pi);
        try { ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, b.build()); } catch (SecurityException e) { }
    }

    static void schedule(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, 0);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 7, new Intent(c, Notify.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        if (!p.getBoolean("on", false)) return;
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, p.getInt("h", 22)); t.set(Calendar.MINUTE, p.getInt("m", 0)); t.set(Calendar.SECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis()) t.add(Calendar.DAY_OF_MONTH, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    @Override public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || "android.intent.action.MY_PACKAGE_REPLACED".equals(a)) { schedule(c); return; }
        SharedPreferences p = c.getSharedPreferences(PREF, 0);
        show(c, 1, p.getString("title", "فلوسي 💰"), p.getString("body", "سجّلت مصاريف النهارده؟"));
    }
}
