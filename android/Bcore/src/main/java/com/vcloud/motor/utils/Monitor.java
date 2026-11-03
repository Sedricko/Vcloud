package com.vcloud.motor.utils;

import android.os.Process;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.vcloud.motor.core.env.BEnvironment;

/**
 * Vcloud: monitor detalhado do que acontece dentro do espaço virtual (telas, serviços, avisos, provedores, chamadas aos serviços do
 * Android e seus erros, saúde de cada processo). Arquivo próprio (blackbox/monitor.log, gira em 4 MB), separado do histórico curto
 * da Central; o relatório inclui o final dele. Chamadas muito frequentes viram um RESUMO a cada 60 s por processo (contagem, erros e
 * tempo médio) em vez de uma linha por chamada; erros e a primeira chamada de cada tipo sempre entram.
 * Nada que pareça segredo (token, senha) é gravado: só o nome do método.
 */
public final class Monitor {
    private static final long MAX_BYTES = 4L * 1024 * 1024;
    private static final SimpleDateFormat HORA = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    private static final class Estat {
        final AtomicLong n = new AtomicLong(), erros = new AtomicLong(), totalNs = new AtomicLong();
        volatile long ultimoErroLog;
    }

    private static final Map<String, Estat> CHAMADAS = new ConcurrentHashMap<>();
    private static final Map<String, Long> JANELA = new ConcurrentHashMap<>();
    private static volatile boolean resumoIniciado;
    private static volatile String processo = "?";

    private Monitor() {}

    public static void processo(String nome) { processo = nome == null ? "?" : nome; }

    private static File arquivo() {
        File d = BEnvironment.getVirtualRoot();
        return new File(d, "monitor.log");
    }

    /** Linha sempre gravada. */
    public static synchronized void ev(String categoria, String texto) {
        boolean eraDentro = DENTRO.get() != null;
        DENTRO.set(Boolean.TRUE);
        try {
            File f = arquivo();
            if (f.length() > MAX_BYTES) {
                File velho = new File(f.getParentFile(), "monitor.old");
                velho.delete();
                f.renameTo(velho);
            }
            try (FileWriter w = new FileWriter(f, true)) {
                w.write(HORA.format(new Date()) + " " + Process.myPid() + " " + processo + " [" + categoria + "] " + texto + "\n");
            }
        } catch (Throwable ignored) {
        } finally {
            if (!eraDentro) DENTRO.remove();
        }
    }

    /** Linha gravada no máximo 1x por janela para a mesma chave (a repetição fica no RESUMO). */
    public static void amostra(String categoria, String chave, long janelaMs, String texto) {
        long agora = System.currentTimeMillis();
        Long antes = JANELA.get(categoria + "|" + chave);
        if (antes != null && agora - antes < janelaMs) return;
        JANELA.put(categoria + "|" + chave, agora);
        ev(categoria, texto);
    }

    /** Uma chamada a um serviço do Android feita por um app de dentro. */
    private static final ThreadLocal<Boolean> DENTRO = new ThreadLocal<>();

    public static void chamada(String servico, String metodo, long ns, Throwable erro, Object[] argsBrutos) {
        // o próprio monitor grava arquivo (passa pelo OsStub): sem esta trava ele registraria a si mesmo
        if (DENTRO.get() != null) return;
        DENTRO.set(Boolean.TRUE);
        try {
            chamadaInterna(servico, metodo, ns, erro, argsBrutos);
        } finally {
            DENTRO.remove();
        }
    }

    private static void chamadaInterna(String servico, String metodo, long ns, Throwable erro, Object[] argsBrutos) {
        // OsStub = cada leitura/escrita/stat de arquivo do app (milhares por segundo, e "arquivo não existe" é normal): só entra na contagem
        if ("OsStub".equals(servico)) {
            Estat o = CHAMADAS.get("OsStub." + metodo);
            if (o == null) { o = new Estat(); Estat v = ((ConcurrentHashMap<String, Estat>) CHAMADAS).putIfAbsent("OsStub." + metodo, o); if (v != null) o = v; }
            o.n.incrementAndGet(); o.totalNs.addAndGet(ns); if (erro != null) o.erros.incrementAndGet();
            iniciarResumo();
            if (ns > 500_000_000L) amostra("LENTO", "OsStub." + metodo, 5_000, "OsStub." + metodo + " " + args(metodo, argsBrutos) + " demorou " + ns / 1_000_000 + " ms");
            return;
        }
        String chave = servico + "." + metodo;
        Estat e = CHAMADAS.get(chave);
        boolean primeira = false;
        if (e == null) { e = new Estat(); Estat velho = ((ConcurrentHashMap<String, Estat>) CHAMADAS).putIfAbsent(chave, e); if (velho != null) e = velho; else primeira = true; }
        e.n.incrementAndGet();
        e.totalNs.addAndGet(ns);
        iniciarResumo();
        if (erro != null) {
            e.erros.incrementAndGet();
            long agora = System.currentTimeMillis();
            if (agora - e.ultimoErroLog > 2_000) {
                e.ultimoErroLog = agora;
                ev("ERRO-SERVICO", chave + " " + args(metodo, argsBrutos) + " -> " + erro.getClass().getSimpleName() + ": " + curto(erro.getMessage(), 160) + " (" + ns / 1_000_000 + " ms)");
            }
        } else if (primeira) {
            ev("SERVICO", chave + " " + args(metodo, argsBrutos) + " (primeira chamada, " + ns / 1_000_000 + " ms)");
        } else if (ns > 500_000_000L) {
            amostra("LENTO", chave, 5_000, chave + " demorou " + ns / 1_000_000 + " ms");
        }
    }

    private static void iniciarResumo() {
        if (resumoIniciado) return;
        synchronized (Monitor.class) {
            if (resumoIniciado) return;
            resumoIniciado = true;
        }
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                while (true) {
                    try { Thread.sleep(60_000); } catch (InterruptedException e) { return; }
                    try { resumir(); saude(); } catch (Throwable ignored) { }
                }
            }
        }, "monitor-resumo");
        t.setDaemon(true);
        t.start();
    }

    private static void resumir() {
        StringBuilder sb = new StringBuilder();
        int itens = 0;
        for (Map.Entry<String, Estat> en : CHAMADAS.entrySet()) {
            Estat e = en.getValue();
            long n = e.n.getAndSet(0), er = e.erros.getAndSet(0), ns = e.totalNs.getAndSet(0);
            if (n == 0) continue;
            if (itens++ < 60) sb.append("\n    ").append(en.getKey()).append(" n=").append(n).append(" erros=").append(er)
                    .append(" médio=").append(ns / n / 1000).append("us");
        }
        if (itens > 0) ev("RESUMO-60s", itens + " tipos de chamada aos serviços do Android" + sb);
    }

    /** Memória, linhas de execução e arquivos abertos do processo. */
    public static void saude() {
        try {
            Runtime rt = Runtime.getRuntime();
            long usado = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024, max = rt.maxMemory() / 1024 / 1024;
            int threads = Thread.getAllStackTraces().size();
            String[] fds = new File("/proc/self/fd").list();
            ev("SAUDE", "memória=" + usado + "/" + max + " MB threads=" + threads + " fds=" + (fds == null ? "?" : fds.length));
        } catch (Throwable ignored) { }
    }

    public static void iniciar() { iniciarResumo(); }

    public static String curto(String s, int n) {
        if (s == null) return "";
        s = s.replace('\n', ' ');
        return s.length() > n ? s.substring(0, n) + "…" : s;
    }

    /** Resumo curto e sem segredos dos argumentos de uma chamada. */
    public static String args(String metodo, Object[] a) {
        if (a == null || a.length == 0) return "()";
        String m = metodo.toLowerCase(Locale.ROOT);
        if (m.contains("token") || m.contains("password") || m.contains("credential") || m.contains("secret")) return "(…)";
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < a.length && i < 5; i++) {
            if (i > 0) sb.append(", ");
            Object o = a[i];
            if (o == null) sb.append("null");
            else if (o instanceof String) sb.append('"').append(curto((String) o, 40)).append('"');
            else if (o instanceof Number || o instanceof Boolean) sb.append(o);
            else if (o instanceof android.content.Intent) {
                android.content.Intent in = (android.content.Intent) o;
                sb.append("Intent{").append(in.getAction()).append(' ').append(in.getComponent() == null ? in.getPackage() : in.getComponent().flattenToShortString()).append('}');
            } else if (o instanceof android.content.ComponentName) sb.append(((android.content.ComponentName) o).flattenToShortString());
            else sb.append(o.getClass().getSimpleName());
        }
        return sb.append(')').toString();
    }

    /** Últimos bytes do monitor (para o relatório). */
    public static String ler(int maxBytes) {
        try {
            File f = arquivo();
            if (!f.exists()) return "";
            long tam = f.length();
            try (java.io.RandomAccessFile r = new java.io.RandomAccessFile(f, "r")) {
                long ini = Math.max(0, tam - maxBytes);
                r.seek(ini);
                byte[] b = new byte[(int) (tam - ini)];
                r.readFully(b);
                String s = new String(b, "UTF-8");
                int nl = ini > 0 ? s.indexOf('\n') : -1;
                return nl >= 0 ? s.substring(nl + 1) : s;
            }
        } catch (Throwable t) { return ""; }
    }
}
