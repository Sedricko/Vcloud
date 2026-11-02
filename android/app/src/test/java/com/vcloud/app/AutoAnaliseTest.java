package com.vcloud.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vcloud.motor.utils.AutoAnalise;
import java.util.List;
import org.junit.Test;

/** Trechos reais dos relatórios do Vcloud (logcat threadtime). */
public class AutoAnaliseTest {
    private static final String LOG =
            "11-01 22:51:10.966  6031  6031 I Zygote  : Process 6031 created for com.vcloud.app:p1\n"
          + "11-01 22:51:13.100  6031  6045 W AccountAuthenticator: java.lang.SecurityException: Only 0p apps are allowed to call this API.\n"
          + "11-01 22:51:13.100  6031  6045 W AccountAuthenticator: \tat atmv.i(:com.google.android.gms@264330035:23)\n"
          + "11-01 22:09:51.092 27072 27082 W AccountAuthenticator: java.lang.SecurityException: Only system may: get application restrictions for other user/app com.google.android.gms\n"
          + "11-01 21:18:13.559 25281 25295 W Binder  : java.lang.SecurityException: Access denied, requires: android.permission.ACCOUNT_MANAGER\n"
          + "11-01 21:18:13.600 25281 25295 W Binder  : java.lang.SecurityException: Access denied, requires: android.permission.ACCOUNT_MANAGER\n"
          + "11-01 22:51:11.795  6004  6153 E Finsky  : java.lang.IllegalStateException: Cannot sync with 0 Android ID\n"
          + "11-01 22:51:11.639  6004  6153 E Finsky  : java.lang.IllegalStateException: Unable to load font Font(GoogleFont(\"Google Sans\"))\n"
          + "11-01 22:51:12.000  6004  6004 I BootstrapClass: java.lang.NoSuchMethodException: dalvik.system.VMRuntime.setHiddenApiExemptions\n"
          + "11-01 22:51:14.000  6500  6500 E AndroidRuntime: FATAL EXCEPTION: main\n"
          + "11-01 22:51:14.000  6500  6500 E AndroidRuntime: Process: com.android.vending, PID: 6500\n"
          + "11-01 22:51:14.000  6500  6500 E AndroidRuntime: java.lang.SecurityException: Not allowed to bind to service Intent { act=x }\n"
          + "11-01 22:51:20.000  6600  6600 E AlgoNovo : coisa que ninguém conhece 12345\n";

    private static AutoAnalise.Achado achar(List<AutoAnalise.Achado> l, String trecho) {
        for (AutoAnalise.Achado a : l) if (a.titulo.contains(trecho) || (a.detalhe != null && a.detalhe.contains(trecho))) return a;
        return null;
    }

    @Test public void achaAsFalhasConhecidas() {
        List<AutoAnalise.Achado> l = AutoAnalise.analisar(LOG, "");
        assertTrue(achar(l, "só aceita app do sistema") != null);
        assertTrue(achar(l, "só aceita o sistema") != null);
        assertTrue(achar(l, "identificador do aparelho") != null);
        assertTrue(achar(l, "Fonte do Google") != null);
        assertTrue(achar(l, "Erro sem regra") != null);
    }

    @Test public void agrupaERepete() {
        AutoAnalise.Achado a = achar(AutoAnalise.analisar(LOG, ""), "android.permission.ACCOUNT_MANAGER");
        assertEquals(2, a.vezes);
        assertEquals("ALTA", a.gravidade);
    }

    @Test public void daNomeAoProcessoEAoFatal() {
        AutoAnalise.Achado a = achar(AutoAnalise.analisar(LOG, ""), "App caiu");
        assertTrue(a.processos.containsKey("com.android.vending"));
        AutoAnalise.Achado z = achar(AutoAnalise.analisar(LOG, ""), "Only 0p");
        assertTrue(z == null || z.processos.containsKey("com.vcloud.app:p1"));
    }

    @Test public void ignoraRuidoConhecido() {
        assertTrue(achar(AutoAnalise.analisar(LOG, ""), "setHiddenApiExemptions") == null);
    }

    @Test public void marcaRecenteSoNosDezMinutosFinais() {
        List<AutoAnalise.Achado> l = AutoAnalise.analisar(LOG, "");
        assertFalse(achar(l, "android.permission.ACCOUNT_MANAGER").recente); // 21:18, o log termina 22:51
        assertTrue(achar(l, "Erro sem regra").recente);
    }

    @Test public void lerOHistorico() {
        String h = "01/11 21:24:27  BIND recusado pelo Android: resultado=-1 | serviço=x\n    linha recuada com Not allowed to bind\n";
        List<AutoAnalise.Achado> l = AutoAnalise.analisar("", h);
        assertEquals(1, l.size());
        assertEquals("histórico do Vcloud", l.get(0).processos.keySet().iterator().next());
    }

    @Test public void vazioENuloNaoQuebram() {
        assertTrue(AutoAnalise.analisar(null, null).isEmpty());
        assertTrue(AutoAnalise.relatorio("", "").contains("Nenhum problema"));
    }
}
