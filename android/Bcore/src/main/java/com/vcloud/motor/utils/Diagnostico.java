package com.vcloud.motor.utils;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
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

    // Freio de laço de quedas: 4 mortes rápidas (< 10 s) do mesmo pacote em 30 s pausam novos inícios dele.
    private static final Map<String, List<Long>> MORTES_RAPIDAS = new java.util.HashMap<>();
    private static final java.util.Set<String> PAUSADOS = new java.util.HashSet<>();
    private static final long JANELA_MS = 30_000;
    private static final int MORTES_PARA_PAUSAR = 4;

    private Diagnostico() {}

    public static void registrarMorteRapida(String pacote) {
        synchronized (MORTES_RAPIDAS) {
            List<Long> l = MORTES_RAPIDAS.get(pacote);
            if (l == null) MORTES_RAPIDAS.put(pacote, l = new java.util.ArrayList<>());
            l.add(System.currentTimeMillis());
        }
    }

    /** true enquanto o pacote está caindo em laço (só nos últimos 30 s). Anota no histórico quando a pausa começa. */
    public static boolean emLaco(String pacote) {
        long agora = System.currentTimeMillis();
        boolean laco;
        synchronized (MORTES_RAPIDAS) {
            List<Long> l = MORTES_RAPIDAS.get(pacote);
            int n = 0;
            if (l != null) {
                for (java.util.Iterator<Long> it = l.iterator(); it.hasNext(); ) {
                    if (agora - it.next() > JANELA_MS) it.remove(); else n++;
                }
            }
            laco = n >= MORTES_PARA_PAUSAR;
            if (!laco) { PAUSADOS.remove(pacote); return false; }
            if (!PAUSADOS.add(pacote)) return true;
        }
        evento("PAUSADO  " + pacote + ": caiu " + MORTES_PARA_PAUSAR + " vezes em 30 s; novos inícios ficam parados até parar de cair");
        return true;
    }

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
        } catch (Throwable ignored) {
            // diagnóstico nunca pode derrubar o motor (nem por erro de inicialização de BEnvironment)
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

    /** Erro de Java visto dentro de um app: uma linha no histórico (o relatório completo só sai pelo botão da Central). */
    public static void erro(String tipo, Throwable t) {
        String onde;
        try { onde = VcloudCore.getAppProcessName(); } catch (Throwable e) { onde = "?"; }
        StackTraceElement[] st = t.getStackTrace();
        evento("ERRO  " + tipo + " em " + onde + "\n    " + t.getClass().getName() + ": " + t.getMessage()
                + (st.length > 0 ? "\n      em " + st[0] : ""));
    }

    private static final Map<String, Long> ULTIMO_ERRO = new ConcurrentHashMap<>();

    /** Erro de Java visto e recuperado pelo motor. O mesmo erro (tipo + mensagem) só entra de novo depois de 30 s. */
    public static void erroCapturado(String tipo, String pacote, boolean recuperado, String mensagem, String primeiraLinha) {
        String msg = CausaDeErro.resumirMensagem(mensagem, 160);
        String chave = tipo + "|" + (pacote == null ? "" : pacote) + "|" + (msg.length() > 80 ? msg.substring(0, 80) : msg);
        long agora = System.currentTimeMillis();
        Long antes = ULTIMO_ERRO.get(chave);
        // Play Store/Play Services de dentro repetem o mesmo erro o tempo todo: no máximo 1 vez a cada 5 minutos
        long janela = pacote != null && infraGoogle(pacote) ? 300_000 : 30_000;
        if (antes != null && agora - antes < janela) return;
        ULTIMO_ERRO.put(chave, agora);
        evento("ERRO  " + tipo + " em " + (pacote == null ? "(o próprio Vcloud)" : pacote)
                + (recuperado ? " (recuperado)" : " (sem recuperação)")
                + "\n    " + msg);
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
        if (System.currentTimeMillis() - ini < 10_000) registrarMorteRapida(pacote);
        new Thread(() -> {
            long seg = (System.currentTimeMillis() - ini) / 1000;
            StringBuilder sb = new StringBuilder("FECHOU  " + pacote + " pid=" + pid + " depois de " + seg + " s");
            if (seg < 15) sb.append("  [fechou logo ao abrir]");
            try { Thread.sleep(1500); } catch (InterruptedException ignored) { /* o registro do Android sai com atraso */ }
            int codigo = codigoDoAndroid(pid);
            sb.append("\n    Android: ").append(codigo < 0 ? "indisponível" : nomeDoMotivo(codigo) + descricao(pid));
            StringBuilder erros = new StringBuilder();
            String causa = causaDoLogcat(pid, erros);
            sb.append("\n    Causa provável: ").append(causa).append(erros);
            if (codigo == ApplicationExitInfo.REASON_ANR) sb.append(pilhaDoAnr(pid));
            evento(sb.toString());
            if (anormal(codigo, seg)) avisar(pacote);
        }, "diagnostico").start();
    }

    // -1 = sem informação (Android antigo, pid desconhecido ou sem registro).
    private static int codigoDoAndroid(int pid) {
        ApplicationExitInfo e = saida(pid);
        return e == null ? -1 : e.getReason();
    }

    private static String descricao(int pid) {
        ApplicationExitInfo e = saida(pid);
        return e == null || e.getDescription() == null ? "" : " (" + e.getDescription() + ")";
    }

    private static ApplicationExitInfo saida(int pid) {
        if (Build.VERSION.SDK_INT < 30 || pid <= 0) return null;
        try {
            Context c = VcloudCore.getContext();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> l = am.getHistoricalProcessExitReasons(c.getPackageName(), pid, 1);
            return l.isEmpty() ? null : l.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    // ANR: o Android guarda o trace; mostra onde a linha principal do app estava presa.
    private static String pilhaDoAnr(int pid) {
        ApplicationExitInfo e = saida(pid);
        if (e == null) return "";
        try (java.io.InputStream in = e.getTraceInputStream()) {
            if (in == null) return "\n    (o Android não guardou o trace deste ANR)";
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0 && bo.size() < 2 * 1024 * 1024) bo.write(b, 0, n);
            StringBuilder sb = new StringBuilder("\n    Onde a linha principal estava presa:");
            for (String l : CausaDeErro.pilhaMain(bo.toString("UTF-8"), 25)) sb.append("\n      ").append(l);
            return sb.toString();
        } catch (Exception ex) {
            return "\n    (não consegui ler o trace do ANR: " + ex.getMessage() + ")";
        }
    }

    // Parar pelo usuário ou pelo Vcloud não é problema; queda, ANR, pouca memória ou fechar logo ao abrir é.
    private static boolean anormal(int codigo, long seg) {
        if (Build.VERSION.SDK_INT >= 30 && codigo >= 0) {
            return codigo == ApplicationExitInfo.REASON_CRASH || codigo == ApplicationExitInfo.REASON_CRASH_NATIVE
                    || codigo == ApplicationExitInfo.REASON_ANR || codigo == ApplicationExitInfo.REASON_LOW_MEMORY
                    || codigo == ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
                    || codigo == ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE;
        }
        return seg < 15;
    }

    private static final Map<String, Long> ULTIMO_AVISO = new ConcurrentHashMap<>();

    // Play Store e Play Services de dentro caem sozinhos o tempo todo: já têm o freio e o histórico, sem aviso na tela.
    private static boolean infraGoogle(String pacote) {
        return pacote.startsWith("com.google.android.") || pacote.equals("com.android.vending");
    }

    // Toast simples (sem permissão de notificação): vale enquanto algum app do Vcloud está na tela. No máximo 1 por minuto por app.
    private static void avisar(String pacote) {
        if (infraGoogle(pacote)) return;
        long agora = System.currentTimeMillis();
        Long antes = ULTIMO_AVISO.get(pacote);
        if (antes != null && agora - antes < 60_000) return;
        ULTIMO_AVISO.put(pacote, agora);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(VcloudCore.getContext(),
                        pacote + " fechou. Veja Configurações > Diagnóstico", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
                // sem janela para mostrar: o registro no diário já existe
            }
        });
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
        StringBuilder tudo = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "threadtime", "--pid=" + pid});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) tudo.append(l).append('\n');
            }
        } catch (Exception e) {
            return "não consegui ler o logcat (" + e.getMessage() + ")";
        }
        String log = tudo.toString();
        for (String l : CausaDeErro.linhas(log)) linhas.append("\n      ").append(l);
        return CausaDeErro.de(log);
    }
}
