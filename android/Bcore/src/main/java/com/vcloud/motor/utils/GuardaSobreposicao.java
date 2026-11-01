package com.vcloud.motor.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.vcloud.motor.core.env.BEnvironment;

/**
 * As janelas flutuantes dos apps de dentro (balão do Unit etc.) são janelas de sistema: o Android as mostra sobre QUALQUER app.
 * Elas não podem sair do Vcloud: o processo ":black" (que enxerga quem está na frente) deixa um sinal em arquivo
 * ({@link #sinalizar}) e cada app de dentro esconde as suas janelas flutuantes enquanto o Vcloud não estiver à vista.
 */
public final class GuardaSobreposicao {
    private static final int[] TIPOS_FLUTUANTES = {
            2038, // TYPE_APPLICATION_OVERLAY
            2002, // TYPE_PHONE
            2003, // TYPE_SYSTEM_ALERT
    };
    private static final Set<View> ESCONDIDAS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean iniciado;
    private static boolean avisouFalha;
    private static Boolean ultimoSinal; // só no ":black": evita escrever no histórico a cada segundo

    private GuardaSobreposicao() {}

    private static File sinal() {
        return new File(BEnvironment.getVirtualRoot(), "fora_do_vcloud");
    }

    /** Chamado pelo ":black" a cada segundo: true = nenhuma tela do Vcloud (nem de app de dentro) está em primeiro plano. */
    public static void sinalizar(boolean fora) {
        try {
            File f = sinal();
            if (fora) f.createNewFile(); else f.delete();
            if (ultimoSinal == null || ultimoSinal != fora) {
                ultimoSinal = fora;
                Diagnostico.evento(fora ? "SOBREPOSIÇÃO escondida: o Vcloud saiu de vista" : "SOBREPOSIÇÃO liberada: o Vcloud voltou");
            }
        } catch (Throwable ignored) {
            // sem o sinal, as janelas só ficam como estavam
        }
    }

    public static boolean estaFora() {
        try { return sinal().exists(); } catch (Throwable e) { return false; }
    }

    /** Chamado em cada app de dentro depois que ele abre. */
    public static synchronized void iniciar() {
        if (iniciado) return;
        iniciado = true;
        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override public void run() {
                try {
                    tique();
                } catch (Throwable e) {
                    if (!avisouFalha) { avisouFalha = true; Diagnostico.evento("SOBREPOSIÇÃO não consegui ler as janelas do app: " + e); }
                }
                h.postDelayed(this, 1000);
            }
        });
    }

    private static void tique() throws Exception {
        boolean fora = estaFora();
        if (!fora && ESCONDIDAS.isEmpty()) return;
        if (fora) {
            for (View v : janelasFlutuantes()) {
                if (v.getVisibility() == View.VISIBLE) { ESCONDIDAS.add(v); v.setVisibility(View.INVISIBLE); }
            }
        } else {
            // só devolve o que NÓS escondemos: se o app já tinha escondido o balão, ele continua escondido
            for (View v : new ArrayList<>(ESCONDIDAS)) {
                if (v.getVisibility() == View.INVISIBLE) v.setVisibility(View.VISIBLE);
            }
            ESCONDIDAS.clear();
        }
    }

    private static List<View> janelasFlutuantes() throws Exception {
        Class<?> c = Class.forName("android.view.WindowManagerGlobal");
        Object global = c.getMethod("getInstance").invoke(null);
        Field fViews = c.getDeclaredField("mViews");
        Field fParams = c.getDeclaredField("mParams");
        Field fLock = c.getDeclaredField("mLock");
        fViews.setAccessible(true); fParams.setAccessible(true); fLock.setAccessible(true);
        List<View> saida = new ArrayList<>();
        synchronized (fLock.get(global)) {
            List<?> views = (List<?>) fViews.get(global);
            List<?> params = (List<?>) fParams.get(global);
            for (int i = 0; i < views.size() && i < params.size(); i++) {
                int tipo = ((WindowManager.LayoutParams) params.get(i)).type;
                for (int t : TIPOS_FLUTUANTES) if (tipo == t) { saida.add((View) views.get(i)); break; }
            }
        }
        return saida;
    }
}
