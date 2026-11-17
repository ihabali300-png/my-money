package com.ihab.money;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 📩 A bank SMS arrived → small notification «سجّله؟». The app reads the inbox itself when opened. */
public class SmsIn extends BroadcastReceiver {
    private static final Pattern SPEND = Pattern.compile("(شراء|خصم|سحب|مدى|Purchase|POS|Debit|دفع)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTP = Pattern.compile("(رمز|OTP|كلمة المرور|code)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMT = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(?:SAR|ر\\.?س|ريال)|(?:SAR|مبلغ|بمبلغ)\\s*:?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    @Override public void onReceive(Context c, Intent i) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(i.getAction())) return;
        StringBuilder b = new StringBuilder();
        for (SmsMessage m : Telephony.Sms.Intents.getMessagesFromIntent(i)) b.append(m.getMessageBody());
        String body = b.toString();
        if (OTP.matcher(body).find() || !SPEND.matcher(body).find()) return;
        Matcher a = AMT.matcher(body);
        String amt = a.find() ? (a.group(1) != null ? a.group(1) : a.group(2)) : null;
        if (amt == null) return;
        Notify.show(c, 2000 + (int) (System.currentTimeMillis() % 1000), "📩 مصروف " + amt + " ريال من البنك", "دوس عشان تسجّله في فلوسي بدوسة");
    }
}
