package com.vcloud.motor.proxy.record;

import android.content.Intent;


public class ProxyBroadcastRecord {
    public Intent mIntent;
    public int mUserId;

    public ProxyBroadcastRecord(Intent intent, int userId) {
        mIntent = intent;
        mUserId = userId;
    }

    public static void saveStub(Intent shadow, Intent target, int userId) {
        shadow.putExtra("_B_|_target_", target);
        shadow.putExtra("_B_|_user_id_", userId);
    }

    public static ProxyBroadcastRecord create(Intent intent) {
        Intent target = intent.getParcelableExtra("_B_|_target_");
        int userId = intent.getIntExtra("_B_|_user_id_", 0);
        // Vcloud: quem dispara o PendingIntent (ex.: IntentSender.sendIntent do instalador, alarme) manda extras "de preenchimento";
        // o Android os põe no intent de fachada, não no original. Sem isto a Play Store nunca via EXTRA_STATUS e a instalação ficava "Instalando...".
        if (target != null) {
            try {
                android.os.Bundle extras = intent.getExtras();
                if (extras != null) {
                    extras = new android.os.Bundle(extras);
                    extras.remove("_B_|_target_");
                    extras.remove("_B_|_user_id_");
                    if (!extras.isEmpty()) target.putExtras(extras);
                }
            } catch (Throwable ignored) { }
        }
        return new ProxyBroadcastRecord(target, userId);
    }

    @Override
    public String toString() {
        return "ProxyBroadcastRecord{" +
                "mIntent=" + mIntent +
                ", mUserId=" + mUserId +
                '}';
    }
}
