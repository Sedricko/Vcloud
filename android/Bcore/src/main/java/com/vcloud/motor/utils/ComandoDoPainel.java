package com.vcloud.motor.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.lang.ref.WeakReference;

/**
 * Comandos do painel lateral para a tela que está na frente de um app de dentro. Tudo acontece DENTRO do app:
 * "Voltar" vai só para a janela dele (nunca para o celular de verdade). Roda na linha principal do app.
 */
public final class ComandoDoPainel {
    public static final int VOLTAR = 1;
    public static final int GIRAR = 2;

    private static WeakReference<Activity> topo = new WeakReference<>(null);

    private ComandoDoPainel() {}

    public static void definirTopo(Activity a) {
        topo = new WeakReference<>(a);
    }

    public static void limparTopo(Activity a) {
        if (topo.get() == a) topo = new WeakReference<>(null);
    }

    public static void executar(int comando) {
        Activity a = topo.get();
        if (a == null || a.isFinishing()) return;
        switch (comando) {
            case VOLTAR: {
                long t = SystemClock.uptimeMillis();
                a.dispatchKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0));
                a.dispatchKeyEvent(new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0));
                break;
            }
            case GIRAR: {
                boolean deitado = a.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                a.setRequestedOrientation(deitado ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            }
            default:
                break;
        }
    }
}
