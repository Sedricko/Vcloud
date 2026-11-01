package com.vcloud.motor.utils;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.core.env.BEnvironment;

/** Vcloud: diário interno (abriu / não abriu / fechou + causa em português). Fica no celular, nada sai. */
public final class Diagnostico {
    private static final long MAX_BYTES = 200 * 1024;
    private static final Map<String, Long> INICIO = new ConcurrentHashMap<>();

    // Primeira regra que casar com o logcat do app vence.
    private static final String[][] CAUSAS = {
            {"ForegroundServiceDidNotStartInTime", "Serviço em primeiro plano não iniciou a tempo"},
            {"UnsatisfiedLinkError", "Biblioteca nativa não carregou (arquitetura/ABI do app?)"},
            {"Google Play services", "Precisa do Google Play Services: ative em Configurações > Serviços do Google"},
            {"GooglePlayServicesUtil", "Precisa do Google Play Services: ative em Configurações > Serviços do Google"},
            {"OutOfMemoryError", "Sem memória"},
            {"Cannot run program", "Programa não encontrado (ex.: su; ligue o Root interno do app)"},
            {"ClassNotFoundException", "Classe não encontrada (o motor não achou código do app)"},
            {"Permission Denial", "Permissão negada pelo Android"},
            {"SecurityException", "Permissão negada pelo Android"},
            {"ANR in", "O app não respondeu (ANR)"},
            {"Fatal signal", "Queda nativa (código C/C++ do app ou do motor)"},
            {"FATAL EXCEPTION", "Erro de Java no app (veja as linhas abaixo)"},
    };

    private Diagnostico() {}

    private static File arquivo() {
        return new File(BEnvironment.getVirtualRoot(), "diagnostico.log");
    }

    public static synchronized void evento(String texto) {
        try {
            File f = arquivo();
            if (f.length() > MAX_BYTES) { // descarta a metade mais antiga
                String s = ler();
                try (FileWriter w = new FileWriter(f, false)) { w.write(s.substring(s.length() / 2)); }
            }
            String hora = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(new Date());
            try (FileWriter w = new FileWriter(f, true)) { w.write(hora + "  " + texto + "\n"); }
        } catch (Exception ignored) {
            // diagnóstico nunca pode derrubar o motor
        }
    }

    public static synchronized String ler() {
        try {
            File f = arquivo();
            if (!f.exists()) return "";
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static synchronized void limpar() {
        arquivo().delete();
    }

    public static void abrindo(String pacote, String processo) {
        INICIO.put(processo, System.currentTimeMillis());
        evento("ABRINDO  " + pacote + " (" + processo + ")");
    }

    public static void naoAbriu(String pacote, String motivo) {
        evento("NÃO ABRIU  " + pacote + ": " + motivo);
    }

    public static void abriu(String pacote, String processo, int pid) {
        evento("ABRIU  " + pacote + " pid=" + pid);
    }

    /** Chamado quando o processo de um app de dentro morre: busca a causa fora da trava do motor. */
    public static void morreu(final String pacote, final String processo, final int pid) {
        final Long ini = INICIO.remove(processo);
        if (ini == null) return; // já registrado
        new Thread(() -> {
            long seg = (System.currentTimeMillis() - ini) / 1000;
            StringBuilder sb = new StringBuilder("FECHOU  " + pacote + " pid=" + pid + " depois de " + seg + " s");
            if (seg < 15) sb.append("  [fechou logo ao abrir]");
            sb.append("\n    Android: ").append(motivoDoAndroid(pid));
            StringBuilder erros = new StringBuilder();
            String causa = causaDoLogcat(pid, erros);
            sb.append("\n    Causa provável: ").append(causa).append(erros);
            evento(sb.toString());
        }, "diagnostico").start();
    }

    private static String motivoDoAndroid(int pid) {
        if (Build.VERSION.SDK_INT < 30 || pid <= 0) return "indisponível";
        try {
            Context c = VcloudCore.getContext();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> l = am.getHistoricalProcessExitReasons(c.getPackageName(), pid, 1);
            if (l.isEmpty()) return "sem registro";
            ApplicationExitInfo e = l.get(0);
            return nomeDoMotivo(e.getReason()) + (e.getDescription() == null ? "" : " (" + e.getDescription() + ")");
        } catch (Exception e) {
            return "indisponível";
        }
    }

    private static String nomeDoMotivo(int r) {
        switch (r) {
            case ApplicationExitInfo.REASON_CRASH: return "erro de Java";
            case ApplicationExitInfo.REASON_CRASH_NATIVE: return "queda nativa";
            case ApplicationExitInfo.REASON_ANR: return "não respondeu (ANR)";
            case ApplicationExitInfo.REASON_LOW_MEMORY: return "morto por pouca memória";
            case ApplicationExitInfo.REASON_SIGNALED: return "encerrado por sinal (parado pelo Vcloud ou pelo sistema)";
            case ApplicationExitInfo.REASON_USER_REQUESTED: return "parado pelo usuário";
            case ApplicationExitInfo.REASON_PERMISSION_CHANGE: return "permissão mudou";
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: return "uso excessivo de recursos";
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE: return "falha ao iniciar";
            default: return "código " + r;
        }
    }

    // Lê o logcat só deste pid; devolve a causa e anota até 3 linhas de erro em 'linhas'.
    private static String causaDoLogcat(int pid, StringBuilder linhas) {
        if (pid <= 0) return "sem pid (o processo nem chegou a nascer)";
        List<String> erros = new ArrayList<>();
        StringBuilder tudo = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "threadtime", "--pid=" + pid});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    tudo.append(l).append('\n');
                    if (erros.size() < 3 && (l.contains("Exception") || l.contains("Error") || l.contains("FATAL")
                            || l.contains("Fatal signal") || l.contains("Cannot run"))) {
                        erros.add(l.length() > 220 ? l.substring(0, 220) : l);
                    }
                }
            }
        } catch (Exception e) {
            return "não consegui ler o logcat (" + e.getMessage() + ")";
        }
        for (String l : erros) linhas.append("\n      ").append(l);
        String s = tudo.toString();
        for (String[] c : CAUSAS) if (s.contains(c[0])) return c[1];
        return s.isEmpty() ? "sem registro no logcat (o sistema pode ter matado o processo)"
                : "não identificada: gere o relatório e mande";
    }
}
