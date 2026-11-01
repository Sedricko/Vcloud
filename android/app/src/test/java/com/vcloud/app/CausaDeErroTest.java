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
        assertTrue(CausaDeErro.de("W: Google Play services missing").contains("Google Play Services"));
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
}
