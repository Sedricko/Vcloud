package com.vcloud.motor.utils;

/** Vcloud: transforma o logcat de um app que fechou numa causa em português. Sem Android: dá para testar. */
public final class CausaDeErro {
    // Primeira regra que casar vence.
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

    private CausaDeErro() {}

    public static String de(String logcat) {
        if (logcat == null || logcat.isEmpty()) return "sem registro no logcat (o sistema pode ter matado o processo)";
        for (String[] c : CAUSAS) if (logcat.contains(c[0])) return c[1];
        return "não identificada: gere o relatório e mande";
    }
}
