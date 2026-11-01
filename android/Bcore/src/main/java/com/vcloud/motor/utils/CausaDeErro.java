package com.vcloud.motor.utils;

import java.util.ArrayList;
import java.util.List;

/** Vcloud: transforma o logcat de um app que fechou numa causa em português. Sem Android: dá para testar. */
public final class CausaDeErro {
    // Dentro do bloco "FATAL EXCEPTION" qualquer regra vale (é o erro que matou o app). Primeira que casar vence.
    private static final String[][] NO_BLOCO_FATAL = {
            {"Bad notification for startForeground", "Notificação do serviço em primeiro plano recusada pelo Android (canal inexistente ou ícone inválido)"},
            {"ForegroundServiceDidNotStartInTime", "Serviço em primeiro plano não iniciou a tempo"},
            {"UnsatisfiedLinkError", "Biblioteca nativa não carregou (arquitetura/ABI do app?)"},
            {"OutOfMemoryError", "Sem memória"},
            {"Cannot run program", "Programa não encontrado (ex.: su; ligue o Root interno do app)"},
            {"ClassNotFoundException", "Classe não encontrada (o motor não achou código do app)"},
            {"SecurityException", "Permissão negada pelo Android"},
            {"Permission Denial", "Permissão negada pelo Android"},
    };
    // No log inteiro só vale o que não costuma aparecer como ruído em apps que seguem vivos.
    private static final String[][] NO_LOG_TODO = {
            {"ForegroundServiceDidNotStartInTime", "Serviço em primeiro plano não iniciou a tempo"},
            {"Cannot run program", "Programa não encontrado (ex.: su; ligue o Root interno do app)"},
            {"UnsatisfiedLinkError", "Biblioteca nativa não carregou (arquitetura/ABI do app?)"},
            {"Fatal signal", "Queda nativa (código C/C++ do app ou do motor)"},
            {"ANR in", "O app não respondeu (ANR)"},
            {"Google Play services is missing", "Precisa do Google Play Services: ative em Configurações > Serviços do Google"},
            {"GooglePlayServicesNotAvailable", "Precisa do Google Play Services: ative em Configurações > Serviços do Google"},
            {"System.exit called", "O app se encerrou sozinho (System.exit); costuma ser checagem de licença ou de ambiente"},
            {"DestroyActivityItem{mActivityToken", "Fechamento normal (a tela do app foi fechada)"},
    };
    private static final String[] RUIDO = {"BootstrapClass", "WorkManagerProxy", "HiddenApi", "setHiddenApiExemptions"};

    private CausaDeErro() {}

    private static String blocoFatal(String logcat) {
        int i = logcat.indexOf("FATAL EXCEPTION");
        return i < 0 ? null : logcat.substring(i, Math.min(logcat.length(), i + 2500));
    }

    public static String de(String logcat) {
        if (logcat == null || logcat.isEmpty()) return "sem registro no logcat (o sistema pode ter matado o processo)";
        String fatal = blocoFatal(logcat);
        if (fatal != null) {
            for (String[] c : NO_BLOCO_FATAL) if (fatal.contains(c[0])) return c[1];
            return "Erro de Java no app (veja as linhas abaixo)";
        }
        for (String[] c : NO_LOG_TODO) if (logcat.contains(c[0])) return c[1];
        return "não identificada: gere o relatório e mande";
    }

    /** Até 3 linhas que mostram o erro: o começo do bloco FATAL, ou as primeiras linhas de erro que não são ruído conhecido. */
    public static List<String> linhas(String logcat) {
        List<String> r = new ArrayList<>();
        if (logcat == null) return r;
        String[] ls = logcat.split("\n");
        for (int i = 0; i < ls.length && r.isEmpty(); i++) {
            if (ls[i].contains("FATAL EXCEPTION")) {
                for (int j = i; j < Math.min(ls.length, i + 3); j++) r.add(corta(ls[j]));
            }
        }
        if (!r.isEmpty()) return r;
        for (String l : ls) {
            if (r.size() >= 3) break;
            if (!(l.contains("Exception") || l.contains("Error") || l.contains("Fatal signal") || l.contains("Cannot run"))) continue;
            boolean ruido = false;
            for (String n : RUIDO) if (l.contains(n)) ruido = true;
            if (!ruido) r.add(corta(l));
        }
        return r;
    }

    private static String corta(String l) {
        return l.length() > 220 ? l.substring(0, 220) : l;
    }
}
