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
        sb.append("\n=== REQUISITOS DO GOOGLE: O QUE EXISTE NO CELULAR x DENTRO DO VCLOUD ===\n");
        try { sb.append(requisitosGoogle(ctx)); } catch (Throwable e) { sb.append("(leitura dos requisitos falhou: ").append(e).append(")\n"); }
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

    private static final String[] PACOTES_GOOGLE = {
            "com.google.android.gms", "com.google.android.gsf", "com.google.android.gsf.login", "com.android.vending",
            "com.google.android.setupwizard", "com.google.android.partnersetup", "com.google.android.configupdater",
            "com.google.android.backuptransport", "com.google.android.backup", "com.google.android.syncadapters.contacts",
            "com.google.android.syncadapters.calendar", "com.google.android.onetimeinitializer", "com.google.android.feedback",
            "com.google.android.webview", "com.android.webview", "com.google.android.play.games"};

    private static String versao(android.content.pm.PackageManager pm, String pkg) {
        try { return "sim, " + pm.getPackageInfo(pkg, 0).versionName; } catch (Throwable e) { return "não"; }
    }

    /** O que o login/Play Store exigem e se existe: pacotes, tipos de conta, telas e serviços de autenticação do gms de dentro. */
    private static String requisitosGoogle(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pacotes (celular | dentro do Vcloud):\n");
        for (String p : PACOTES_GOOGLE) {
            String fora = versao(ctx.getPackageManager(), p);
            String dentro;
            try {
                PackageInfo pi = com.vcloud.motor.VcloudCore.getBPackageManager().getPackageInfo(p, 0, 0);
                dentro = pi == null ? "não" : "sim, " + pi.versionName;
            } catch (Throwable e) { dentro = "não"; }
            boolean falta = fora.startsWith("sim") && dentro.equals("não");
            sb.append("  ").append(p).append("  |  ").append(fora).append("  |  ").append(dentro)
                    .append(falta ? "   <- EXISTE NO CELULAR E FALTA DENTRO" : "").append('\n');
        }
        // Exigências lidas no APK da Play Store 53.8.11 (strings do código): provedor de contas do gms, serviços do gsf/gms de login e checkin.
        sb.append("\nO que a Play Store procura (existe dentro do Vcloud?):\n");
        String[] autoridades = {"com.google.android.gms.auth.accounts", "com.google.android.gsf.gservices", "com.google.android.gms.chimera",
                "com.google.android.gms.phenotype", "com.google.android.gms.fonts", "com.google.android.gms.common.appdoctor"};
        for (String a : autoridades) {
            boolean dentro;
            try { dentro = com.vcloud.motor.VcloudCore.getBPackageManager().resolveContentProvider(a, 0, 0) != null; } catch (Throwable e) { dentro = false; }
            sb.append("  provedor ").append(a).append(": ").append(dentro ? "sim" : "NÃO").append('\n');
        }
        String[] acoes = {"com.google.android.gsf.action.GET_GLS", "com.google.android.gms.checkin.BIND_TO_SERVICE",
                "android.accounts.AccountAuthenticator", "com.google.android.gms.auth.aang.events.services.START"};
        for (String a : acoes) {
            boolean dentro;
            try { dentro = com.vcloud.motor.VcloudCore.getBPackageManager().resolveService(new android.content.Intent(a), 0, null, 0) != null; } catch (Throwable e) { dentro = false; }
            sb.append("  serviço (ação) ").append(a).append(": ").append(dentro ? "sim" : "NÃO").append('\n');
        }
        try {
            android.accounts.AuthenticatorDescription[] t = com.vcloud.motor.fake.frameworks.BAccountManager.get().getAuthenticatorTypes();
            sb.append("\nTipos de conta registrados dentro: ");
            if (t == null || t.length == 0) sb.append("NENHUM");
            else for (android.accounts.AuthenticatorDescription d : t) sb.append(d.type).append(" (").append(d.packageName).append(") ");
            sb.append('\n');
        } catch (Throwable e) { sb.append("\n(tipos de conta: não consegui ler: ").append(e).append(")\n"); }

        try {
            int flags = android.content.pm.PackageManager.GET_ACTIVITIES | android.content.pm.PackageManager.GET_SERVICES
                    | android.content.pm.PackageManager.GET_PROVIDERS | android.content.pm.PackageManager.GET_RECEIVERS
                    | android.content.pm.PackageManager.GET_PERMISSIONS;
            PackageInfo g = com.vcloud.motor.VcloudCore.getBPackageManager().getPackageInfo("com.google.android.gms", flags, 0);
            if (g == null) { sb.append("\nGoogle Play Services não está dentro do Vcloud.\n"); return sb.toString(); }
            sb.append("\nGoogle Play Services de dentro: ")
                    .append(g.activities == null ? 0 : g.activities.length).append(" telas, ")
                    .append(g.services == null ? 0 : g.services.length).append(" serviços, ")
                    .append(g.providers == null ? 0 : g.providers.length).append(" provedores, ")
                    .append(g.receivers == null ? 0 : g.receivers.length).append(" receptores, ")
                    .append(g.requestedPermissions == null ? 0 : g.requestedPermissions.length).append(" permissões pedidas.\n");
            java.util.regex.Pattern chave = java.util.regex.Pattern.compile("(?i)auth|login|signin|addaccount|account|uiflows");
            sb.append("Telas de login/conta do gms (nome | exportada | ligada):\n");
            int n = 0;
            if (g.activities != null) for (android.content.pm.ActivityInfo a : g.activities) {
                if (n >= 30) break;
                if (chave.matcher(a.name).find() && !a.name.contains("Chimera")) { sb.append("  ").append(a.name).append(" | ").append(a.exported).append(" | ").append(a.enabled).append('\n'); n++; }
            }
            sb.append("Serviços de login/conta do gms (nome | exportado | permissão exigida):\n");
            n = 0;
            if (g.services != null) for (android.content.pm.ServiceInfo v : g.services) {
                if (n >= 30) break;
                if (chave.matcher(v.name).find()) { sb.append("  ").append(v.name).append(" | ").append(v.exported).append(" | ").append(v.permission).append('\n'); n++; }
            }
        } catch (Throwable e) { sb.append("(componentes do gms: não consegui ler: ").append(e).append(")\n"); }
        return sb.toString();
    }
}
