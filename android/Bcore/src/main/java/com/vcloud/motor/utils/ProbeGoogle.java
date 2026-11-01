package com.vcloud.motor.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import com.vcloud.motor.VcloudCore;

/** Medições do Google (GSF/GMS/Play Store) para achar por que a Play Store de dentro fica sem identificador (android-id = 0). */
public final class ProbeGoogle {
    private static final Uri GSERVICES = Uri.parse("content://com.google.android.gsf.gservices");
    private static final String[] PACOTES = {"com.google.android.gsf", "com.google.android.gms", "com.android.vending"};

    private ProbeGoogle() {}

    /** Faz a MESMA consulta que a Play Store faz ao Gservices e diz exatamente o que voltou. */
    public static String consultarAndroidId(Context ctx) {
        try (Cursor c = ctx.getContentResolver().query(GSERVICES, null, null, new String[]{"android_id"}, null)) {
            if (c == null) return "a consulta devolveu null (provedor indisponível ou negado)";
            if (!c.moveToFirst()) return "o provedor respondeu, mas sem a linha android_id (colunas=" + c.getColumnCount() + ")";
            String v = c.getColumnCount() > 1 ? c.getString(1) : c.getString(0);
            return "android_id=" + v;
        } catch (Throwable e) {
            return "erro na consulta: " + e;
        }
    }

    private static String versao(Context ctx, String pacote, boolean dentro) {
        try {
            PackageInfo pi = dentro ? VcloudCore.getBPackageManager().getPackageInfo(pacote, 0, 0) : ctx.getPackageManager().getPackageInfo(pacote, 0);
            return pi == null ? "não instalado" : "versão " + pi.versionName;
        } catch (Throwable e) {
            return "não instalado";
        }
    }

    /** Relatório para a Central (roda no processo principal do Vcloud). */
    public static String relatorio(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permissão READ_GSERVICES do Vcloud: ")
                .append(ctx.checkSelfPermission("com.google.android.providers.gsf.permission.READ_GSERVICES") == 0 ? "concedida" : "NEGADA").append('\n');
        String sec;
        try { sec = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID); } catch (Throwable e) { sec = "erro " + e; }
        sb.append("ANDROID_ID do Vcloud (Settings.Secure): ").append(sec).append('\n');
        sb.append("Gservices pelo Vcloud (celular real): ").append(consultarAndroidId(ctx)).append('\n');
        for (String p : PACOTES) {
            sb.append(p).append(" | celular: ").append(versao(ctx, p, false)).append(" | dentro do Vcloud: ").append(versao(ctx, p, true)).append('\n');
        }
        return sb.toString();
    }

    /** Dentro da Play Store/Play Services: mede o caminho que ELA usa (com os ganchos do motor) e anota no histórico. */
    public static void medirDentro(final Context app, final String pacote) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000); // deixa o app terminar de abrir
                Diagnostico.evento("GOOGLE dentro de " + pacote + ": Gservices -> " + consultarAndroidId(app));
            } catch (Throwable e) {
                Diagnostico.evento("GOOGLE dentro de " + pacote + ": a medição falhou: " + e);
            }
        }, "medir-google");
        t.setDaemon(true);
        t.start();
    }
}
