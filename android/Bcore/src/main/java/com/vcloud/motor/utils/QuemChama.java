package com.vcloud.motor.utils;

import android.os.Process;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

import com.vcloud.motor.core.env.BEnvironment;

/**
 * Vcloud: qual app de dentro é o dono de um pid. O servidor grava um arquivo por processo de app (pids/PID = appId virtual) e
 * os apps de dentro leem. Serve para responder Binder.getCallingUid() com o chamador DE VERDADE de cada chamada
 * (antes valia o app que criou o processo, que fica velho e dava "Unknown calling package name").
 */
public final class QuemChama {
    private QuemChama() {}

    private static final long VALE_MS = 3_000;
    private static final ConcurrentHashMap<Integer, long[]> CACHE = new ConcurrentHashMap<>();

    private static File dir() { return new File(BEnvironment.getVirtualRoot(), "pids"); }

    /** Servidor: um app de dentro passou a ser o dono deste pid. */
    public static void registrar(int pid, int appUidVirtual) {
        if (pid <= 0) return;
        try {
            dir().mkdirs();
            Files.write(new File(dir(), String.valueOf(pid)).toPath(), String.valueOf(appUidVirtual).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) { }
        CACHE.remove(pid);
    }

    public static void remover(int pid) {
        if (pid <= 0) return;
        try { new File(dir(), String.valueOf(pid)).delete(); } catch (Throwable ignored) { }
        CACHE.remove(pid);
    }

    /** Servidor, ao ligar: pids de antes não valem mais (o número pode ter sido reusado). */
    public static void limparTudo() {
        File[] fs = dir().listFiles();
        if (fs != null) for (File f : fs) f.delete();
        CACHE.clear();
    }

    /** uid virtual do app de dentro dono do pid; 0 se o pid não é de app de dentro (tela, servidor, sistema) ou é este processo. */
    public static int uidDoPid(int pid) {
        if (pid <= 0 || pid == Process.myPid()) return 0;
        long agora = System.currentTimeMillis();
        long[] c = CACHE.get(pid);
        if (c != null && c[1] > agora) return (int) c[0];
        int uid = 0;
        try {
            File f = new File(dir(), String.valueOf(pid));
            if (f.isFile()) uid = Integer.parseInt(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
        } catch (Throwable ignored) { }
        CACHE.put(pid, new long[]{uid, agora + VALE_MS});
        return uid;
    }
}
