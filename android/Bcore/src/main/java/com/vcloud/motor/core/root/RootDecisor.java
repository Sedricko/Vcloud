package com.vcloud.motor.core.root;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** Decide um pedido de root do "su". Java puro: quem pergunta ao usuário entra como Perguntador. */
public final class RootDecisor {
    public interface Perguntador {
        /** "allow", "once", "deny", ou null se o usuário não respondeu a tempo. */
        String perguntar(String pkg, String comando);
    }

    /** Pedido lido do socket: 3 linhas "VSU1", "pkg <dica>", "cmd <comando>". */
    public static final class Pedido {
        public final String dicaPacote;
        public final String comando;

        Pedido(String dicaPacote, String comando) {
            this.dicaPacote = dicaPacote;
            this.comando = comando;
        }

        public static Pedido ler(BufferedReader r) throws IOException {
            String v = r.readLine();
            if (!"VSU1".equals(v)) throw new IOException("protocolo desconhecido");
            String pkg = r.readLine();
            String cmd = r.readLine();
            if (pkg == null || cmd == null || !pkg.startsWith("pkg ") && !pkg.equals("pkg") || !cmd.startsWith("cmd")) {
                throw new IOException("pedido incompleto");
            }
            return new Pedido(pkg.length() > 4 ? pkg.substring(4) : "", cmd.length() > 4 ? cmd.substring(4) : "");
        }
    }

    private static final ConcurrentHashMap<String, Object> TRAVAS = new ConcurrentHashMap<>();

    private RootDecisor() {}

    /** Devolve "ALLOW" ou "DENY <motivo>" (a linha que o su espera) e registra o pedido. */
    public static String decidir(RootPolicy p, String pkg, String comando, Perguntador quem) {
        if (!p.geralLigado()) {
            p.registrar(pkg == null ? "?" : pkg, "NEGADO (root desligado)", comando);
            return "DENY root desligado no painel do Vcloud";
        }
        if (!RootPolicy.pacoteValido(pkg)) {
            p.registrar("?", "NEGADO (app não identificado)", comando);
            return "DENY app nao identificado";
        }
        // Um app pode pedir várias vezes ao mesmo tempo: uma janela por vez, e quem espera já vê a resposta.
        synchronized (TRAVAS.computeIfAbsent(pkg, k -> new Object())) {
            switch (p.estado(pkg)) {
                case PERMITIDO:
                    p.registrar(pkg, "PERMITIDO", comando);
                    return "ALLOW";
                case NEGADO:
                    p.registrar(pkg, "NEGADO (painel)", comando);
                    return "DENY negado no painel do Vcloud";
                default:
                    break;
            }
            p.limparResposta(pkg);
            String r = quem.perguntar(pkg, comando);
            if ("allow".equals(r)) { p.registrar(pkg, "PERMITIDO (sempre)", comando); return "ALLOW"; }
            if ("once".equals(r)) { p.registrar(pkg, "PERMITIDO (uma vez)", comando); return "ALLOW"; }
            if ("deny".equals(r)) { p.registrar(pkg, "NEGADO (usuário)", comando); return "DENY negado pelo usuario"; }
            p.registrar(pkg, "NEGADO (sem resposta)", comando);
            return "DENY sem resposta a tempo";
        }
    }

    /** Lê o PPid do texto de /proc/<pid>/status; -1 se não achar. */
    public static int ppidDe(String status) {
        if (status == null) return -1;
        for (String l : status.split("\n")) {
            if (l.startsWith("PPid:")) {
                try { return Integer.parseInt(l.substring(5).trim()); } catch (NumberFormatException e) { return -1; }
            }
        }
        return -1;
    }
}
