package com.vcloud.app;

import static org.junit.Assert.assertTrue;

import com.vcloud.motor.utils.CausaDeErro;
import org.junit.Test;

public class CausaDeErroTest {
    @Test public void vazioNaoQuebra() {
        assertTrue(CausaDeErro.de("").startsWith("sem registro"));
        assertTrue(CausaDeErro.de(null).startsWith("sem registro"));
    }

    @Test public void reconheceAsCausasConhecidas() {
        assertTrue(CausaDeErro.de("W: Google Play services is missing").contains("Google Play Services"));
        assertTrue(CausaDeErro.de("java.io.IOException: Cannot run program \"su\"").contains("su"));
        assertTrue(CausaDeErro.de("F libc: Fatal signal 11 (SIGSEGV)").startsWith("Queda nativa"));
        assertTrue(CausaDeErro.de("java.lang.UnsatisfiedLinkError: dlopen failed").contains("nativa"));
    }

    @Test public void regraMaisEspecificaVenceAGenerica() {
        // "FATAL EXCEPTION" aparece junto com a causa real: a causa real tem de ganhar.
        assertTrue(CausaDeErro.de("FATAL EXCEPTION: main\nCannot run program \"su\"").contains("su"));
    }

    @Test public void semPistaPedeRelatorio() {
        assertTrue(CausaDeErro.de("tudo normal").startsWith("não identificada"));
    }

    @Test public void notificacaoRuimNoBlocoFatal() {
        String log = "W BootstrapClass: NoSuchMethodException setHiddenApiExemptions\n"
                + "E AndroidRuntime: FATAL EXCEPTION: main\n"
                + "E AndroidRuntime: Process: dev.unit.app, PID: 1\n"
                + "E AndroidRuntime: android.app.RemoteServiceException$CannotPostForegroundServiceNotificationException: Bad notification for startForeground\n";
        assertTrue(CausaDeErro.de(log).startsWith("Notificação do serviço"));
        assertTrue(CausaDeErro.linhas(log).get(0).contains("FATAL EXCEPTION"));
    }

    @Test public void saidaPropriaEFechamentoNormal() {
        assertTrue(CausaDeErro.de("I m.vcloud.app:p0: System.exit called, status: 0").startsWith("O app se encerrou sozinho"));
        assertTrue(CausaDeErro.de("E HCallbackStub:     DestroyActivityItem{mActivityToken=x,finished=true}").startsWith("Fechamento normal"));
        // só citar "Google Play services" (app vivo) não é causa
        assertTrue(CausaDeErro.de("I DynamiteModule: Google Play services client").startsWith("não identificada"));
    }

    @Test public void ruidoSemFatalNaoViraPermissaoNegada() {
        String log = "W Foo: SecurityException ignorada\nW BootstrapClass: Caused by: java.lang.NoSuchMethodException\n";
        assertTrue(CausaDeErro.de(log).startsWith("não identificada"));
        assertTrue(CausaDeErro.linhas("W BootstrapClass: Caused by: java.lang.NoSuchMethodException\n").isEmpty()); // ruído conhecido
    }

    @Test public void causasNovas() {
        assertTrue(CausaDeErro.de("java.io.IOException: Cannot run program \"su\": error=2").contains("Root interno"));
        assertTrue(CausaDeErro.de("Cannot run program \"/data/x/lib02.so\": error=13, Permission denied").contains("error=13"));
        assertTrue(CausaDeErro.de("F m.vcloud.app:p8: JNI DETECTED ERROR IN APPLICATION: x").contains("proteção"));
        assertTrue(CausaDeErro.de("E AndroidRuntime: FATAL EXCEPTION\nSecurityException: Not allowed to bind to service Intent").contains("serviço do Google"));
    }

    @Test public void pilhaDaLinhaPrincipalNoAnr() {
        String trace = "\"Signal Catcher\" daemon prio=10\n  at x\n\n\"main\" prio=5 tid=1 Blocked\n  at a.B.c(B.java:1)\n  at d.E.f(E.java:2)\n\n\"Other\" prio=5\n  at z\n";
        java.util.List<String> p = CausaDeErro.pilhaMain(trace, 25);
        assertTrue(p.size() == 3 && p.get(0).startsWith("\"main\"") && p.get(2).contains("E.f"));
        assertTrue(CausaDeErro.pilhaMain(null, 5).isEmpty());
    }

    @Test public void freioDeLacoDeQuedas() {
        String pkg = "teste.laco";
        assertTrue(!com.vcloud.motor.utils.Diagnostico.emLaco(pkg));
        for (int i = 0; i < 3; i++) com.vcloud.motor.utils.Diagnostico.registrarMorteRapida(pkg);
        assertTrue(!com.vcloud.motor.utils.Diagnostico.emLaco(pkg)); // 3 ainda não pausa
        com.vcloud.motor.utils.Diagnostico.registrarMorteRapida(pkg);
        assertTrue(com.vcloud.motor.utils.Diagnostico.emLaco(pkg));  // a 4ª pausa (o histórico falha em silêncio fora do Android)
        assertTrue(!com.vcloud.motor.utils.Diagnostico.emLaco("outro.pacote")); // só o pacote que cai
    }

    @Test public void contaSoOsFechamentosCausadosPeloAndroid() {
        String h = "01/11 14:38:14  FECHOU  a pid=1 depois de 540 s\n    Android: morto por pouca memória ()\n"
                + "01/11 14:39:02  FECHOU  b pid=2 depois de 557 s\n    Android: parado pelo usuário ([REMOVE TASK])\n"
                + "01/11 14:40:00  FECHOU  c pid=3 depois de 30 s\n    Android: uso excessivo de recursos ()\n";
        assertTrue(CausaDeErro.contarEncerradosPeloAndroid(h) == 2);
        assertTrue(CausaDeErro.contarEncerradosPeloAndroid(null) == 0);
        assertTrue(CausaDeErro.contarEncerradosPeloAndroid("") == 0);
    }

    @Test public void mensagemDeErroViraUmaLinhaCurta() {
        String pilha = "Native stack trace:\n#00 pc 0x7fea3 /apex/x/libhttpengine.so\n#01 pc 0x68e6f /apex/x/libhttpengine.so";
        assertTrue(CausaDeErro.resumirMensagem(pilha, 160).equals("Native stack trace:"));
        assertTrue(CausaDeErro.resumirMensagem("a".repeat(300), 10).equals("aaaaaaaaaa…"));
        assertTrue(CausaDeErro.resumirMensagem(null, 10).isEmpty());
    }
}
