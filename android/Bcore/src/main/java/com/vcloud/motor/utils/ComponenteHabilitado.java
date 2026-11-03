package com.vcloud.motor.utils;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.vcloud.motor.core.env.BEnvironment;

/**
 * Vcloud: estado "habilitado/desabilitado" de telas dos apps de dentro. O motor ignorava `android:enabled="false"` e as chamadas
 * setComponentEnabledSetting, então um activity-alias desligado de propósito (ex.: o ponto de entrada do Play Protect da Play Store)
 * era achado e abria a tela errada, em ciclo com o gms. Estado gravado em arquivo (cada app tem vários processos).
 * shortcut: só filtra a busca por intent implícito de telas; serviços/receptores seguem como antes.
 */
public final class ComponenteHabilitado {
    private ComponenteHabilitado() {}

    private static File arquivo(String pacote, String classe) {
        return new File(new File(BEnvironment.getVirtualRoot(), "componentes/" + pacote), classe);
    }

    /** estado do PackageManager: 0 padrão, 1 habilitado, 2 desabilitado (outros contam como desabilitado). */
    public static void definir(ComponentName c, int estado) {
        if (c == null) return;
        try {
            File f = arquivo(c.getPackageName(), c.getClassName());
            if (estado == 0) { f.delete(); return; }
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), String.valueOf(estado).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) { }
    }

    /** -1 se ninguém mexeu. */
    public static int estado(String pacote, String classe) {
        try {
            File f = arquivo(pacote, classe);
            if (f.isFile()) return Integer.parseInt(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
        } catch (Throwable ignored) { }
        return -1;
    }

    public static boolean ativo(ActivityInfo ai) {
        if (ai == null) return false;
        int e = estado(ai.packageName, ai.name);
        if (e == 1) return true;
        if (e > 1) return false;
        return ai.enabled;
    }
}
