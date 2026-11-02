package com.vcloud.motor.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vcloud: lê o logcat inteiro (todos os processos do Vcloud: tela, servidor ":black" e apps de dentro) e o histórico
 * e devolve os problemas encontrados, em português, do mais grave ao menos grave, com a causa e a linha que prova.
 * Java puro (sem Android): dá para testar. As regras vêm de falhas reais já vistas nos relatórios.
 */
public final class AutoAnalise {
    private AutoAnalise() {}

    private static final String ALTA = "ALTA", MEDIA = "MÉDIA", INFO = "INFO";

    /** {id, gravidade, texto que casa, título, causa provável, o que fazer}. Primeira que casar na linha vence. */
    private static final String[][] REGRAS = {
            {"perm", ALTA, "Access denied, requires:", "Permissão de sistema negada a um serviço de dentro",
                    "O serviço de dentro exigiu uma permissão que só o sistema tem e quem chamou foi o Vcloud.", "Conceder essa permissão no gancho de permissões do motor (checkPermissionForDevice)."},
            {"soSistema", ALTA, "Only system may", "O Android só aceita o sistema nessa chamada",
                    "O app de dentro pediu algo de outro usuário/app (ex.: restrições de app) e o Android recusou.", "Trocar o pacote/usuário da chamada pelos do Vcloud no gancho."},
            {"zeroP", ALTA, "Only 0p apps", "O Google recusou o chamador (só aceita app do sistema/Google)",
                    "O serviço de login do Google confere quem o chamou.", "O chamador precisa aparecer como sistema (uid 1000) só nessa chamada."},
            {"bind", ALTA, "Not allowed to bind to service", "Não conseguiu ligar a um serviço",
                    "O pedido foi ao Android real, que recusou; o serviço é de dentro do Vcloud.", "Fazer o bind pelo serviço-ponte do Vcloud."},
            {"uid", ALTA, "doesn't match source uid", "Uid de origem diferente do uid de quem chamou (provedor)",
                    "O provedor conferiu a origem do pedido e o uid virtual não bate com o real.", "Usar o uid do celular na origem (ContentProviderStub/NativeCore)."},
            {"androidId", ALTA, "Unexpected android-id = 0", "Google de dentro sem identificador do aparelho (android-id = 0)",
                    "O Google de dentro ainda não se registrou (checkin) ou o identificador foi negado.", "Ver se o GMS de dentro faz o checkin; ver 'not approved to access the device ID'."},
            {"androidId", ALTA, "Cannot sync with 0 Android ID", "Google de dentro sem identificador do aparelho (android-id = 0)",
                    "O Google de dentro ainda não se registrou (checkin) ou o identificador foi negado.", "Ver se o GMS de dentro faz o checkin; ver 'not approved to access the device ID'."},
            {"androidId", ALTA, "not approved to access the device ID", "Google de dentro sem identificador do aparelho (android-id = 0)",
                    "O Google de dentro recusou dar o identificador ao Vcloud (pacote não aprovado).", "Ver como o GMS de dentro decide quem é aprovado (pacote/uid)."},
            {"usuarios", ALTA, "MANAGE_USERS", "O Android recusou uma pergunta sobre usuários (isMainUser etc.)",
                    "Pergunta de usuário feita com o uid do Vcloud, sem permissão.", "Resposta neutra no gancho do UserManager (já feito)."},
            {"dpm", ALTA, "Caller with uid", "O Android recusou o pacote informado (política de dispositivo)",
                    "O pacote do app de dentro não é do uid do Vcloud.", "Resposta neutra no gancho do DevicePolicyManager (já feito)."},
            {"biometria", ALTA, "not allowed to perform USE_BIOMETRIC", "A confirmação biométrica/tela de bloqueio foi recusada",
                    "O nome do pacote da chamada não é do uid do Vcloud.", "Trocar o nome do app pelo do Vcloud (IAuthServiceProxy, já feito)."},
            {"bioAvancada", ALTA, "SET_BIOMETRIC_DIALOG_ADVANCED", "A janela biométrica pediu recursos avançados (logo/conteúdo)",
                    "O sistema exige permissão de sistema para logo/conteúdo personalizado na janela de confirmação.", "Tirar esses itens do pedido (IAuthServiceProxy, já feito)."},
            {"pkgUid", ALTA, "does not belong to", "Pacote não pertence ao uid de quem chamou",
                    "O app de dentro se apresentou com o nome dele e o Android conferiu com o uid do Vcloud.", "Trocar o nome do app pelo do Vcloud na chamada (como no PackageInstaller)."},
            {"pkgChamador", ALTA, "Unknown calling package name", "O Google não reconheceu o app que o chamou",
                    "O uid que o Google viu como chamador não é o do app que chamou (uid de quem chama velho ou errado).", "Responder o chamador real pelo pid (QuemChama)."},
            {"instanciar", ALTA, "Unable to instantiate activity", "Tela do app não pôde ser criada",
                    "A classe da tela não existe no código do app (pode ser um atalho 'activity-alias') ou o carregador de código falhou.", "Ver 'ATIVIDADE alias' / 'ATIVIDADE não instanciou' no histórico."},
            {"jni", ALTA, "JNI DETECTED ERROR", "O app detecta espaço virtual (erro de JNI dele)",
                    "Proteção do próprio app; não se resolve sem burlar a proteção.", "Não mexer; avisar o usuário."},
            {"nativa", ALTA, "UnsatisfiedLinkError", "Biblioteca nativa não carregou", "Arquitetura/ABI do app ou arquivo ausente.", "Conferir a ABI do app e as bibliotecas copiadas."},
            {"memoria", ALTA, "OutOfMemoryError", "Sem memória", "O processo passou do limite de memória.", "Fechar outros apps de dentro."},
            {"anr", ALTA, "ANR in", "App não respondeu (ANR)", "A thread principal travou.", "Ver o trecho de pilha capturado junto."},
            {"sinal", ALTA, "Fatal signal", "Queda nativa (código C/C++)", "Falha no código nativo do app, do Android ou do motor.", "Ver 'Abort message' e a pilha nativa."},
            {"fonte", MEDIA, "Unable to load font", "Fonte do Google não carregou",
                    "O provedor de fontes do Google de dentro não entregou o arquivo.", "Não impede o app; é visual."},
            {"arquivoProv", MEDIA, "Can't open content://", "Arquivo de provedor não abriu", "O provedor não devolveu o arquivo pedido.", "Ver o provedor citado."},
            {"semPacote", MEDIA, "NameNotFoundException", "App/pacote não encontrado", "O app de dentro perguntou por um pacote que não existe no celular/Vcloud.", "Normalmente ignorável; se repetir, ver o pacote."},
            {"negado", MEDIA, "Permission Denial", "Permissão negada pelo Android", "Um pedido do app de dentro foi recusado.", "Ver a linha de prova."},
            {"seguranca", MEDIA, "SecurityException", "Permissão negada (SecurityException)", "Um pedido do app de dentro foi recusado.", "Ver a linha de prova."},
            {"selinux", MEDIA, "avc:  denied", "Bloqueio do SELinux", "O Android bloqueou um acesso a arquivo/serviço.", "Ver o alvo (scontext/tcontext)."},
    };

    // Regras sobre o histórico do Vcloud (linhas que o próprio motor escreve).
    private static final String[][] REGRAS_HISTORICO = {
            {"h_bindfalhou", ALTA, "BIND virtual falhou", "Bind virtual falhou", "O motor tentou ligar ao serviço de dentro e deu erro.", "Ver o motivo na linha."},
            {"h_bindrecusado", ALTA, "BIND recusado pelo Android", "Bind recusado pelo Android", "O Android devolveu resultado negativo ao ligar o serviço-ponte.", "Ver serviço e ponte na linha."},
            {"h_bindsemservico", MEDIA, "BIND sem serviço de dentro", "Serviço pedido não existe dentro do Vcloud", "O app pediu um serviço que não está instalado dentro.", "Instalar o app dono do serviço ou ignorar."},
            {"h_naoabriu", ALTA, "NÃO ABRIU", "App não abriu", "O motor não conseguiu iniciar o app.", "Ver o motivo na linha."},
            {"h_pausado", ALTA, "PAUSADO", "App pausado por cair em laço", "Caiu 4 vezes em 30 s; novos inícios ficam parados.", "Corrigir a causa da queda."},
            {"h_provengolido", MEDIA, "PROVEDOR erro engolido", "Erro de provedor escondido pelo motor", "O motor engoliu um erro de uid/permissão e devolveu valor padrão.", "Ver provedor e erro."},
            {"h_ativalias", INFO, "ATIVIDADE alias", "Atalho de tela aberto pelo alvo", "O motor abriu o alvo de um activity-alias.", "Informativo."},
    };

    private static final String[] RUIDO = {"BootstrapClass", "WorkManagerProxy", "HiddenApi", "setHiddenApiExemptions", "NativeCrashPrevention",
            "Large parcel", "TransactionThrottler", "PackageParsing", "SimpleCrashFix"};

    private static final Pattern LINHA = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d)\\.\\d+\\s+(\\d+)\\s+(\\d+) ([VDIWEF]) (.*?)\\s*: (.*)$");
    private static final Pattern PROC_CRIADO = Pattern.compile("^Process (\\d+) created for (\\S+)");
    private static final Pattern PROC_FATAL = Pattern.compile("^Process: (\\S+), PID: (\\d+)");
    private static final Pattern PERM = Pattern.compile("requires: ([\\w.]+)");
    private static final Pattern SINAL = Pattern.compile("Fatal signal (\\d+) \\((\\w+)\\)");
    private static final Pattern HIST = Pattern.compile("^(\\d\\d)/(\\d\\d) (\\d\\d:\\d\\d:\\d\\d)  .*");
    private static final Pattern PACOTE_NF = Pattern.compile("NameNotFoundException: ([\\w.]+)");

    /** Um problema agrupado (mesma regra + mesmo detalhe). */
    public static final class Achado {
        public String gravidade, titulo, causa, sugestao, detalhe, primeira, ultima, prova;
        public int vezes;
        public final Map<String, Integer> processos = new LinkedHashMap<>();
        public boolean recente;
    }

    /** Linhas de texto prontas para o relatório. */
    public static String relatorio(String logcat, String historico) {
        List<Achado> achados = analisar(logcat, historico);
        int linhas = logcat == null ? 0 : logcat.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        int conhecidos = 0;
        for (Achado a : achados) if (!a.titulo.startsWith("Erro sem regra")) conhecidos++;
        sb.append("Linhas de log lidas: ").append(linhas).append(". Problemas conhecidos: ").append(conhecidos)
                .append(" (erros sem regra: só os 8 mais novos abaixo).\n");
        sb.append("[novo] = aconteceu nos 10 minutos finais do log (vem primeiro). Sem [novo] = coisa antiga, ainda no registro do Android.\n\n");
        if (achados.isEmpty()) sb.append("Nenhum problema conhecido encontrado.\n");
        int outros = 0;
        for (Achado a : achados) {
            if (a.titulo.startsWith("Erro sem regra") && ++outros > 8) continue;
            sb.append('[').append(a.gravidade).append(']').append(a.recente ? " [novo] " : " ").append(a.titulo);
            if (a.detalhe != null) sb.append(" — ").append(a.detalhe);
            sb.append("\n    ").append(a.vezes).append("x, primeira ").append(a.primeira).append(", última ").append(a.ultima);
            if (!a.processos.isEmpty()) sb.append(", em ").append(String.join(", ", a.processos.keySet()));
            sb.append("\n    Causa provável: ").append(a.causa);
            sb.append("\n    O que fazer: ").append(a.sugestao);
            sb.append("\n    Prova: ").append(a.prova).append("\n\n");
        }
        return sb.toString();
    }

    public static List<Achado> analisar(String logcat, String historico) {
        Map<String, Achado> grupos = new LinkedHashMap<>();
        Map<Integer, String> nomes = new HashMap<>();
        String[] ls = logcat == null ? new String[0] : logcat.split("\n");

        // 1ª passada: quem é cada pid.
        for (String l : ls) {
            Matcher m = LINHA.matcher(l);
            if (!m.matches()) continue;
            Matcher c = PROC_CRIADO.matcher(m.group(6));
            if (c.find()) nomes.put(Integer.parseInt(c.group(1)), c.group(2));
            Matcher f = PROC_FATAL.matcher(m.group(6));
            if (f.find()) nomes.put(Integer.parseInt(f.group(2)), f.group(1));
        }

        String fim = "";
        for (int i = 0; i < ls.length; i++) {
            Matcher m = LINHA.matcher(ls[i]);
            if (!m.matches()) continue;
            fim = m.group(1);
            String msg = m.group(6), nivel = m.group(4);
            if (ehRuido(ls[i])) continue;
            int pid = Integer.parseInt(m.group(2));
            String proc = nomes.containsKey(pid) ? nomes.get(pid) : "pid " + pid;

            if (msg.startsWith("FATAL EXCEPTION")) {
                StringBuilder det = new StringBuilder();
                for (int j = i + 1; j < Math.min(ls.length, i + 6); j++) {
                    Matcher n = LINHA.matcher(ls[j]);
                    if (!n.matches() || !n.group(2).equals(m.group(2))) continue;
                    String t = n.group(6);
                    if (t.startsWith("Process:")) { Matcher f = PROC_FATAL.matcher(t); if (f.find()) proc = f.group(1); continue; }
                    if (t.startsWith("\tat ")) break;
                    det.append(det.length() == 0 ? "" : " | ").append(corta(t, 160));
                    if (det.length() > 0 && !t.startsWith("Caused by")) break;
                }
                somar(grupos, "fatal|" + proc + "|" + corta(det.toString(), 80), ALTA, "App caiu com erro de Java", "Exceção não tratada no app de dentro.", "Ver a exceção ao lado.",
                        corta(det.toString(), 170), m.group(1), ls[i], proc);
                continue;
            }
            String[] r = casar(REGRAS, msg);
            if (r == null) {
                boolean pilha = msg.startsWith("\t") || msg.startsWith("at ") || msg.startsWith("... ") || msg.trim().isEmpty();
                if (!pilha && (nivel.equals("E") || nivel.equals("F"))) somar(grupos, "outro|" + m.group(5) + "|" + corta(msg, 60), INFO, "Erro sem regra conhecida (" + m.group(5) + ")",
                        "Ainda não classificado.", "Se repetir muito ou travar algo, criar uma regra.", corta(msg, 120), m.group(1), ls[i], proc);
                continue;
            }
            String det = null;
            if (r[0].equals("perm")) { Matcher p = PERM.matcher(msg); if (p.find()) det = p.group(1); }
            if (r[0].equals("semPacote")) { Matcher p = PACOTE_NF.matcher(msg); if (p.find()) det = p.group(1); }
            if (r[0].equals("sinal")) { Matcher p = SINAL.matcher(msg); if (p.find()) det = "sinal " + p.group(1) + " (" + p.group(2) + ")"; }
            somar(grupos, r[0] + "|" + det, r[1], r[3], r[4], r[5], det, m.group(1), ls[i], proc);
        }

        if (historico != null) {
            for (String l : historico.split("\n")) {
                Matcher h = HIST.matcher(l);
                if (!h.matches()) continue; // linhas recuadas são detalhe do evento anterior
                String[] r = casar(REGRAS_HISTORICO, l);
                if (r == null) continue;
                String resto = l.substring(l.indexOf("  ") + 2).trim();
                somar(grupos, r[0] + "|" + corta(resto, 50), r[1], r[3], r[4], r[5], corta(resto, 120),
                        h.group(2) + "-" + h.group(1) + " " + h.group(3), l, "histórico do Vcloud");
            }
        }

        List<Achado> lista = new ArrayList<>(grupos.values());
        int fimS = segundos(fim);
        for (Achado a : lista) a.recente = fimS > 0 && fimS - segundos(a.ultima) <= 600 && segundos(a.ultima) > 0;
        Collections.sort(lista, (x, y) -> {
            if (x.recente != y.recente) return x.recente ? -1 : 1; // o registro do Android guarda coisa antiga: o recente vem primeiro
            int g = peso(y.gravidade) - peso(x.gravidade);
            if (g != 0) return g;
            return y.vezes - x.vezes;
        });
        return lista;
    }

    private static void somar(Map<String, Achado> g, String chave, String grav, String titulo, String causa, String sug, String det,
                              String quando, String linhaProva, String proc) {
        Achado a = g.get(chave);
        if (a == null) {
            a = new Achado();
            a.gravidade = grav; a.titulo = titulo; a.causa = causa; a.sugestao = sug; a.detalhe = det == null || det.isEmpty() ? null : det;
            a.primeira = quando; a.prova = corta(linhaProva.trim(), 230);
            g.put(chave, a);
        }
        a.vezes++;
        a.ultima = quando;
        a.processos.merge(proc, 1, Integer::sum);
    }

    private static String[] casar(String[][] regras, String texto) {
        for (String[] r : regras) if (texto.contains(r[2])) return r;
        return null;
    }

    private static boolean ehRuido(String l) {
        for (String n : RUIDO) if (l.contains(n)) return true;
        return false;
    }

    private static int peso(String g) { return g.equals(ALTA) ? 3 : g.equals(MEDIA) ? 2 : 1; }

    private static String corta(String s, int max) { return s.length() <= max ? s : s.substring(0, max) + "…"; }

    /** "MM-DD HH:MM:SS" -> segundos aproximados (basta para diferença de minutos); 0 se não entender. */
    static int segundos(String t) {
        if (t == null || t.length() < 14) return 0;
        try {
            int mes = Integer.parseInt(t.substring(0, 2)), dia = Integer.parseInt(t.substring(3, 5));
            int h = Integer.parseInt(t.substring(6, 8)), mi = Integer.parseInt(t.substring(9, 11)), s = Integer.parseInt(t.substring(12, 14));
            return (((mes * 31 + dia) * 24 + h) * 60 + mi) * 60 + s;
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
