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

    /**
     * Uma vez por "marca": apaga os dados do Google de dentro (gms, gsf, Play Store) para sair de um estado velho/quebrado
     * (tentativas de checkin que caíram antes). Só com o modo Google completo ligado; não mexe no Google do celular.
     */
    public static void reiniciarDadosUmaVez(String marca) {
        try {
            if (!completoDentro()) return;
            File m = new File(BEnvironment.getVirtualRoot(), marca);
            if (m.exists()) return;
            for (String pkg : new String[]{"com.google.android.gms", "com.google.android.gsf", "com.android.vending"}) {
                try { com.vcloud.motor.VcloudCore.get().clearPackage(pkg, 0); } catch (Throwable e) {
                    Diagnostico.evento("GOOGLE reinício: não consegui limpar " + pkg + " (" + e + ")");
                }
            }
            m.createNewFile();
            Diagnostico.evento("GOOGLE dados de dentro reiniciados uma vez (" + marca + "): gms, gsf e Play Store");
        } catch (Throwable e) {
            Diagnostico.evento("GOOGLE reinício falhou: " + e);
        }
    }
}
