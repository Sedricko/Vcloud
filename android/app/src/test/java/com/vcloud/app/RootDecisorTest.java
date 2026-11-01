package com.vcloud.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.vcloud.motor.core.root.RootDecisor;
import com.vcloud.motor.core.root.RootPolicy;
import com.vcloud.motor.core.root.RootPolicy.Estado;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RootDecisorTest {
    private static RootPolicy nova() throws IOException {
        return new RootPolicy(Files.createTempDirectory("vroot").toFile());
    }

    private static final RootDecisor.Perguntador NUNCA = (pkg, cmd) -> { throw new AssertionError("não devia perguntar"); };

    @Test public void estadosDaPolitica() throws Exception {
        RootPolicy p = nova();
        assertEquals(Estado.PERGUNTAR, p.estado("dev.unit.app"));
        p.definir("dev.unit.app", Estado.PERMITIDO);
        assertEquals(Estado.PERMITIDO, p.estado("dev.unit.app"));
        p.definir("dev.unit.app", Estado.NEGADO);
        assertEquals(Estado.NEGADO, p.estado("dev.unit.app"));
        p.definir("dev.unit.app", Estado.PERGUNTAR);
        assertEquals(Estado.PERGUNTAR, p.estado("dev.unit.app"));
    }

    @Test public void arquivoVazioDaVersaoAntigaContaComoPermitido() throws Exception {
        File dir = Files.createTempDirectory("vroot").toFile();
        assertTrue(new File(dir, "dev.tinker.app").createNewFile());
        assertEquals(Estado.PERMITIDO, new RootPolicy(dir).estado("dev.tinker.app"));
    }

    @Test public void nomeDePacoteNaoEscapaDaPasta() throws Exception {
        assertTrue(RootPolicy.pacoteValido("dev.unit.app"));
        assertFalse(RootPolicy.pacoteValido("../x"));
        assertFalse(RootPolicy.pacoteValido("a/b"));
        assertFalse(RootPolicy.pacoteValido(""));
        assertFalse(RootPolicy.pacoteValido(null));
        assertFalse(RootPolicy.pacoteValido(".oculto"));
        try { nova().definir("../fora", Estado.PERMITIDO); throw new AssertionError("devia recusar"); }
        catch (IllegalArgumentException esperado) { /* ok */ }
    }

    @Test public void chaveGeralDesligaParaTodos() throws Exception {
        RootPolicy p = nova();
        p.definir("dev.unit.app", Estado.PERMITIDO);
        p.ligarGeral(false);
        assertTrue(RootDecisor.decidir(p, "dev.unit.app", "id", NUNCA).startsWith("DENY"));
        p.ligarGeral(true);
        assertEquals("ALLOW", RootDecisor.decidir(p, "dev.unit.app", "id", NUNCA));
    }

    @Test public void appNaoIdentificadoENegado() throws Exception {
        RootPolicy p = nova();
        assertTrue(RootDecisor.decidir(p, null, "id", NUNCA).startsWith("DENY"));
        assertTrue(RootDecisor.decidir(p, "../x", "id", NUNCA).startsWith("DENY"));
    }

    @Test public void permitidoENegadoNaoPerguntam() throws Exception {
        RootPolicy p = nova();
        p.definir("a.b", Estado.PERMITIDO);
        p.definir("c.d", Estado.NEGADO);
        assertEquals("ALLOW", RootDecisor.decidir(p, "a.b", "id", NUNCA));
        assertTrue(RootDecisor.decidir(p, "c.d", "id", NUNCA).startsWith("DENY"));
    }

    @Test public void perguntarUsaARespostaDoUsuario() throws Exception {
        RootPolicy p = nova();
        assertEquals("ALLOW", RootDecisor.decidir(p, "a.b", "id", (k, c) -> "allow"));
        assertEquals("ALLOW", RootDecisor.decidir(p, "a.b", "id", (k, c) -> "once"));
        assertEquals(Estado.PERGUNTAR, p.estado("a.b")); // "uma vez" não grava nada
        assertTrue(RootDecisor.decidir(p, "a.b", "id", (k, c) -> "deny").startsWith("DENY"));
        assertTrue(RootDecisor.decidir(p, "a.b", "id", (k, c) -> null).startsWith("DENY")); // sem resposta = nega
    }

    @Test public void respostaDaJanelaFicaSalvaEESoUmaVezConsumida() throws Exception {
        RootPolicy p = nova();
        p.responder("a.b", "allow");
        assertEquals(Estado.PERMITIDO, p.estado("a.b"));
        assertEquals("allow", p.aguardarResposta("a.b", 500));
        assertNull(p.aguardarResposta("a.b", 200)); // já consumida
        p.responder("a.b", "once");
        assertEquals(Estado.PERMITIDO, p.estado("a.b")); // "once" não muda a política
        try { p.responder("a.b", "talvez"); throw new AssertionError("devia recusar"); }
        catch (IllegalArgumentException esperado) { /* ok */ }
    }

    @Test public void doisPedidosJuntosGeramUmaJanelaSo() throws Exception {
        final RootPolicy p = nova();
        final AtomicInteger perguntas = new AtomicInteger();
        final RootDecisor.Perguntador q = (pkg, cmd) -> {
            perguntas.incrementAndGet();
            try { Thread.sleep(300); } catch (InterruptedException e) { /* ok */ }
            p.responder(pkg, "allow"); // a janela grava a política
            return "allow";
        };
        final String[] r = new String[2];
        Thread t1 = new Thread(() -> r[0] = RootDecisor.decidir(p, "a.b", "id", q));
        Thread t2 = new Thread(() -> r[1] = RootDecisor.decidir(p, "a.b", "ls", q));
        t1.start(); t2.start(); t1.join(); t2.join();
        assertEquals("ALLOW", r[0]);
        assertEquals("ALLOW", r[1]);
        assertEquals(1, perguntas.get());
    }

    @Test public void leOPedidoDoSu() throws Exception {
        RootDecisor.Pedido ok = RootDecisor.Pedido.ler(new BufferedReader(new StringReader("VSU1\npkg dev.unit.app\ncmd id -u\n")));
        assertEquals("dev.unit.app", ok.dicaPacote);
        assertEquals("id -u", ok.comando);
        RootDecisor.Pedido vazio = RootDecisor.Pedido.ler(new BufferedReader(new StringReader("VSU1\npkg \ncmd (shell interativo)\n")));
        assertEquals("", vazio.dicaPacote);
        try { RootDecisor.Pedido.ler(new BufferedReader(new StringReader("GET / HTTP/1.1\n"))); throw new AssertionError("devia recusar"); }
        catch (IOException esperado) { /* ok */ }
        try { RootDecisor.Pedido.ler(new BufferedReader(new StringReader("VSU1\npkg a\n"))); throw new AssertionError("devia recusar"); }
        catch (IOException esperado) { /* ok */ }
    }

    @Test public void achaOPaiNoStatusDoProc() {
        assertEquals(1234, RootDecisor.ppidDe("Name:\tsu\nPid:\t99\nPPid:\t1234\nUid:\t10005\n"));
        assertEquals(-1, RootDecisor.ppidDe("Name:\tsu\n"));
        assertEquals(-1, RootDecisor.ppidDe(null));
    }

    @Test public void registroGuardaELimpa() throws Exception {
        RootPolicy p = nova();
        p.registrar("a.b", "PERMITIDO", "id\nls");
        String s = p.lerRegistro();
        assertTrue(s.contains("a.b") && s.contains("PERMITIDO") && s.contains("id ls") && !s.contains("id\nls"));
        p.limparRegistro();
        assertEquals("", p.lerRegistro());
    }

    @Test public void respostaAutomaticaValeParaQuemNaoTemDecisaoPropria() throws Exception {
        RootPolicy p = nova();
        assertEquals(Estado.PERGUNTAR, p.modoAutomatico());
        p.definirModoAutomatico(Estado.PERMITIDO);
        assertEquals("ALLOW", RootDecisor.decidir(p, "a.b", "id", NUNCA)); // não pergunta
        p.definirModoAutomatico(Estado.NEGADO);
        assertTrue(RootDecisor.decidir(p, "a.b", "id", NUNCA).startsWith("DENY"));
        // decisão própria do app vence a automática
        p.definir("c.d", Estado.PERMITIDO);
        assertEquals("ALLOW", RootDecisor.decidir(p, "c.d", "id", NUNCA));
        p.definirModoAutomatico(Estado.PERMITIDO);
        p.definir("e.f", Estado.NEGADO);
        assertTrue(RootDecisor.decidir(p, "e.f", "id", NUNCA).startsWith("DENY"));
        // chave geral desligada vence tudo
        p.ligarGeral(false);
        assertTrue(RootDecisor.decidir(p, "a.b", "id", NUNCA).startsWith("DENY"));
        p.ligarGeral(true);
        p.definirModoAutomatico(Estado.PERGUNTAR);
        assertEquals(Estado.PERGUNTAR, p.modoAutomatico());
    }

    @Test public void avisoLigaEDesliga() throws Exception {
        RootPolicy p = nova();
        assertFalse(p.avisoLigado());
        p.ligarAviso(true);
        assertTrue(p.avisoLigado());
        p.ligarAviso(false);
        assertFalse(p.avisoLigado());
    }
}
