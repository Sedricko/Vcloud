package com.vcloud.motor.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.PowerManager;
import android.provider.Settings;

import java.io.File;
import java.util.List;

import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.core.env.BEnvironment;

/**
 * Vcloud: auto diagnóstico = leitura do estado do Vcloud agora (apps de dentro, processos, chaves, permissões)
 * + análise automática do logcat e do histórico (AutoAnalise). Sai no topo do relatório.
 */
public final class AutoDiagnostico {
    private AutoDiagnostico() {}

    public static String gerar(Context ctx, String logcat) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LEITURA DO VCLOUD AGORA ===\n");
        try { sb.append(estado(ctx)); } catch (Throwable e) { sb.append("(leitura do estado falhou: ").append(e).append(")\n"); }
        sb.append("\n=== ANÁLISE AUTOMÁTICA DO QUE ACONTECEU ===\n");
        try { sb.append(AutoAnalise.relatorio(logcat, Diagnostico.ler())); } catch (Throwable e) { sb.append("(análise falhou: ").append(e).append(")\n"); }
        return sb.toString();
    }

    private static String estado(Context ctx) {
        StringBuilder sb = new StringBuilder();
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            sb.append("Vcloud ").append(pi.versionName).append(" (").append(pi.getLongVersionCode()).append(")\n");
        } catch (Throwable ignored) { }

        File raiz = BEnvironment.getVirtualRoot();
        sb.append("Google completo dentro do Vcloud: ").append(GoogleModo.completoDentro() ? "LIGADO" : "desligado").append('\n');
        sb.append("Root geral desligado: ").append(com.vcloud.motor.core.root.RootPolicy.padrao().geralLigado() ? "não" : "sim").append('\n');
        sb.append("Painel/sobreposição fora do Vcloud: ").append(new File(raiz, "fora_do_vcloud").exists() ? "sim" : "não").append('\n');
        sb.append("Sobrepor a outros apps (permissão): ").append(Settings.canDrawOverlays(ctx) ? "concedida" : "NEGADA").append('\n');
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            sb.append("Bateria sem restrição para o Vcloud: ").append(pm.isIgnoringBatteryOptimizations(ctx.getPackageName()) ? "sim" : "NÃO (o Android pode encerrar apps de dentro)").append('\n');
        } catch (Throwable ignored) { }
        long livre = raiz.getUsableSpace() / (1024 * 1024);
        sb.append("Espaço livre onde fica o Vcloud: ").append(livre).append(" MB").append(livre < 500 ? "  <- POUCO" : "").append('\n');

        sb.append("\nApps instalados dentro do Vcloud:\n");
        try {
            List<PackageInfo> apps = VcloudCore.get().getInstalledPackages(0, 0);
            for (PackageInfo p : apps) {
                sb.append("  ").append(p.packageName).append("  ").append(p.versionName == null ? "?" : p.versionName).append('\n');
            }
            if (apps.isEmpty()) sb.append("  (nenhum)\n");
        } catch (Throwable e) { sb.append("  (não consegui ler: ").append(e).append(")\n"); }

        sb.append("\nProcessos do Vcloud rodando agora (tela, servidor ':black' e apps de dentro):\n");
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            int n = 0;
            for (ActivityManager.RunningAppProcessInfo r : am.getRunningAppProcesses()) {
                if (!r.processName.startsWith(ctx.getPackageName())) continue;
                n++;
                sb.append("  pid ").append(r.pid).append("  ").append(r.processName).append("  importância ").append(r.importance).append('\n');
            }
            if (n == 0) sb.append("  (nenhum visível)\n");
        } catch (Throwable e) { sb.append("  (não consegui ler: ").append(e).append(")\n"); }
        return sb.toString();
    }
}
