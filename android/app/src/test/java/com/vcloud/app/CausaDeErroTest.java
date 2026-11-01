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
}
