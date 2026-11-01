package com.vcloud.motor.core.root;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import com.vcloud.motor.core.env.BEnvironment;

/**
 * Política do root interno do Vcloud, guardada em arquivos (a Central e o servidor rodam em processos diferentes).
 *   _off            existe = root desligado para todos os apps (chave geral do painel)
 *   _auto           "allow" ou "deny" = resposta automática para apps sem decisão própria; sem arquivo = perguntar
 *   _aviso          existe = mostra uma mensagem na tela quando um app usa o root
 *   <pacote>        "allow" ou "deny"; sem arquivo = perguntar ao usuário (arquivo vazio = "allow", versão antiga)
 *   <pacote>.reply  resposta da janela "Permitir root?": allow | once | deny
 *   log.txt         registro dos pedidos
 * Java puro (sem Android) para poder ser testado.
 */
public final class RootPolicy {
    public enum Estado { PERGUNTAR, PERMITIDO, NEGADO }

    private static final Pattern PACOTE = Pattern.compile("[A-Za-z0-9._]{1,200}");
    private static final long MAX_LOG = 100 * 1024;

    private final File dir;

    public RootPolicy(File dir) {
        this.dir = dir;
        dir.mkdirs();
    }

    public static RootPolicy padrao() {
        return new RootPolicy(new File(BEnvironment.getVirtualRoot(), "root"));
    }

    /** O nome do pacote vira nome de arquivo: só aceita letras, números, ponto e sublinhado (sem "/" nem ".."). */
    public static boolean pacoteValido(String pkg) {
        return pkg != null && PACOTE.matcher(pkg).matches() && !pkg.contains("..") && !pkg.startsWith(".");
    }

    private File arquivo(String pkg) {
        if (!pacoteValido(pkg)) throw new IllegalArgumentException("pacote inválido: " + pkg);
        return new File(dir, pkg);
    }

    public boolean geralLigado() {
        return !new File(dir, "_off").exists();
    }

    public void ligarGeral(boolean ligado) {
        File f = new File(dir, "_off");
        try {
            if (ligado) f.delete(); else f.createNewFile();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Resposta automática para app sem decisão própria: PERGUNTAR (padrão), PERMITIDO ou NEGADO para todos. */
    public Estado modoAutomatico() {
        File f = new File(dir, "_auto");
        if (!f.isFile()) return Estado.PERGUNTAR;
        try {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            if (s.equals("allow")) return Estado.PERMITIDO;
            if (s.equals("deny")) return Estado.NEGADO;
        } catch (Exception e) {
            // na dúvida, pergunta
        }
        return Estado.PERGUNTAR;
    }

    public void definirModoAutomatico(Estado e) {
        File f = new File(dir, "_auto");
        try {
            if (e == Estado.PERGUNTAR) { f.delete(); return; }
            try (FileWriter w = new FileWriter(f, false)) { w.write(e == Estado.PERMITIDO ? "allow" : "deny"); }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean avisoLigado() {
        return new File(dir, "_aviso").exists();
    }

    public void ligarAviso(boolean ligado) {
        File f = new File(dir, "_aviso");
        try {
            if (ligado) f.createNewFile(); else f.delete();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Estado estado(String pkg) {
        File f = arquivo(pkg);
        if (!f.isFile()) return Estado.PERGUNTAR;
        try {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            if (s.equals("deny")) return Estado.NEGADO;
            return Estado.PERMITIDO; // "allow" ou vazio (versão antiga)
        } catch (Exception e) {
            return Estado.PERGUNTAR; // na dúvida, pergunta
        }
    }

    public void definir(String pkg, Estado e) {
        File f = arquivo(pkg);
        try {
            if (e == Estado.PERGUNTAR) { f.delete(); return; }
            try (FileWriter w = new FileWriter(f, false)) { w.write(e == Estado.PERMITIDO ? "allow" : "deny"); }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ---- resposta da janela "Permitir root?" ----

    public void limparResposta(String pkg) {
        new File(dir, arquivo(pkg).getName() + ".reply").delete();
    }

    /** Chamado pela janela. "allow" e "deny" também ficam salvos como política do app. */
    public void responder(String pkg, String resposta) {
        if (!resposta.equals("allow") && !resposta.equals("once") && !resposta.equals("deny")) {
            throw new IllegalArgumentException("resposta inválida: " + resposta);
        }
        if (resposta.equals("allow")) definir(pkg, Estado.PERMITIDO);
        if (resposta.equals("deny")) definir(pkg, Estado.NEGADO);
        try (FileWriter w = new FileWriter(new File(dir, arquivo(pkg).getName() + ".reply"), false)) {
            w.write(resposta);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Espera a janela responder; null se passar do tempo. A resposta é consumida. */
    public String aguardarResposta(String pkg, long timeoutMs) {
        File f = new File(dir, arquivo(pkg).getName() + ".reply");
        long fim = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < fim) {
            if (f.isFile()) {
                try {
                    String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                    if (!s.isEmpty()) { f.delete(); return s; }
                } catch (Exception ignored) {
                    // escrita ainda em andamento: tenta de novo
                }
            }
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        }
        return null;
    }

    // ---- registro ----

    public synchronized void registrar(String pkg, String resultado, String comando) {
        try {
            File f = new File(dir, "log.txt");
            if (f.length() > MAX_LOG) { // descarta a metade mais antiga
                String s = lerRegistro();
                try (FileWriter w = new FileWriter(f, false)) { w.write(s.substring(s.length() / 2)); }
            }
            String hora = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(new Date());
            String c = comando == null ? "" : comando.replace('\n', ' ');
            if (c.length() > 200) c = c.substring(0, 200);
            try (FileWriter w = new FileWriter(f, true)) { w.write(hora + "  " + pkg + "  " + resultado + "  " + c + "\n"); }
        } catch (Exception ignored) {
            // o registro nunca pode impedir a decisão
        }
    }

    public synchronized String lerRegistro() {
        try {
            File f = new File(dir, "log.txt");
            return f.isFile() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public synchronized void limparRegistro() {
        new File(dir, "log.txt").delete();
    }
}
