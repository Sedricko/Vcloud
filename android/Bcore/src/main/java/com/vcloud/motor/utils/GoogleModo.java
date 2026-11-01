package com.vcloud.motor.utils;

import java.io.File;

import com.vcloud.motor.core.env.BEnvironment;

/** Chave "Google completo dentro do Vcloud" (arquivo, para valer em todos os processos). Desligada = comportamento antigo. */
public final class GoogleModo {
    private GoogleModo() {}

    private static File arq() {
        return new File(BEnvironment.getVirtualRoot(), "google_completo");
    }

    public static boolean completoDentro() {
        try { return arq().exists(); } catch (Throwable e) { return false; }
    }

    public static void definir(boolean ligado) {
        try {
            if (ligado) arq().createNewFile(); else arq().delete();
        } catch (Throwable e) {
            Diagnostico.evento("GOOGLE modo: não consegui gravar a chave (" + e + ")");
        }
    }
}
