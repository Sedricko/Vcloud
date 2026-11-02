package com.vcloud.motor.core;

import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import dalvik.system.DexFile;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.utils.compat.DexFileCompat;

import com.vcloud.motor.core.system.JarManager;
import com.vcloud.motor.utils.Slog;

public class NativeCore {
    public static final String TAG = "NativeCore";

    static {

        System.loadLibrary("blackbox");
    }

    public static native void init(int apiLevel);

    public static native void enableIO();

    public static native void addIORule(String targetPath, String relocatePath);

    public static native void hideXposed();

    public static native boolean disableHiddenApi();
    
    public static native boolean disableResourceLoading();

    @Keep
    public static int getCallingUid(int origCallingUid) {
        try {
            
            if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
                return origCallingUid;

            if (origCallingUid > Process.LAST_APPLICATION_UID)
                return origCallingUid;

            if (origCallingUid == VcloudCore.getHostUid()) {
                // Vcloud: checagem da origem do provedor (AttributionSource) compara com o uid do celular
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                for (int i = 0; i < st.length && i < 20; i++) {
                    String cn = st[i].getClassName();
                    if (cn.equals("android.content.AttributionSource") || cn.startsWith("android.content.ContentProvider$Transport")) {
                        return origCallingUid;
                    }
                }
                // Vcloud: o chamador de verdade desta chamada (pelo pid), e não o app que criou este processo (fica velho)
                // Mesmo app (chamada dentro do próprio processo ou entre processos do mesmo app, ex.: gms -> gms): no Android de verdade o
                // uid de quem chama é igual a Process.myUid() (= uid do celular aqui). O Google confere isso ("0p": getCallingUid() == myUid()).
                // Só nos apps de dentro; o servidor ":black" e a tela do Vcloud seguem como antes.
                if (VcloudCore.get().isBlackProcess()) {
                    int cpid = android.os.Binder.getCallingPid();
                    if (cpid == Process.myPid()) return Process.myUid(); // igual ao uid que o app vê de si (Play Store: "Binding only allowed within app")
                    int quem = com.vcloud.motor.utils.QuemChama.uidDoPid(cpid);
                    if (quem > 0 && quem < Process.LAST_APPLICATION_UID) {
                        int res = quem == VcloudCore.getBUid() ? Process.myUid() : quem;
                        try {
                            com.vcloud.motor.utils.Diagnostico.eventoLimitado("calluid|" + cpid, 20_000, "UID chamada de outro processo: orig=" + origCallingUid
                                    + " cpid=" + cpid + " quem=" + quem + " eu(buid)=" + VcloudCore.getBUid() + " myUid=" + Process.myUid() + " -> " + res);
                        } catch (Throwable ignored) { }
                        return res;
                    }
                    try {
                        com.vcloud.motor.utils.Diagnostico.eventoLimitado("calluid0|" + cpid, 20_000, "UID chamada de outro processo SEM dono: orig=" + origCallingUid
                                + " cpid=" + cpid + " quem=" + quem + " myUid=" + Process.myUid());
                    } catch (Throwable ignored) { }
                }
                
                String appPackageName = VcloudCore.getAppPackageName();
                if (appPackageName != null && appPackageName.equals("com.google.android.gms")){
                    
                }

                if (appPackageName != null && appPackageName.equals("com.google.android.webview")){
                    return Process.myUid();
                }

                try {
                    int callingBUid = VcloudCore.getCallingBUid();
                    if (callingBUid > 0 && callingBUid < Process.LAST_APPLICATION_UID) {
                        return callingBUid == VcloudCore.getBUid() ? origCallingUid : callingBUid;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting calling BUid: " + e.getMessage());
                }

                try {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (StackTraceElement element : stackTrace) {
                        String className = element.getClassName();
                        String methodName = element.getMethodName();

                        if ((className.contains("Settings") || className.contains("FeatureFlag")) &&
                            (methodName.contains("getString") || methodName.contains("getInt") || 
                             methodName.contains("getLong") || methodName.contains("getFloat"))) {
                            
                            Log.d(TAG, "System settings access detected, using system UID to prevent mismatch");
                            
                            return Process.SYSTEM_UID; 
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error analyzing stack trace for UID resolution: " + e.getMessage());
                }

                return VcloudCore.getHostUid();
            }
            return origCallingUid;
        } catch (Exception e) {
            Log.e(TAG, "Error in getCallingUid: " + e.getMessage());
            
            return Process.myUid();
        }
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static long[] loadEmptyDex() {
        try {
            File emptyJar = JarManager.getInstance().getEmptyJar();
            if (emptyJar == null) {
                Log.w(TAG, "Empty JAR not available, attempting sync initialization");
                JarManager.getInstance().initializeSync();
                emptyJar = JarManager.getInstance().getEmptyJar();
            }
            
            if (emptyJar == null || !emptyJar.exists()) {
                Log.e(TAG, "Empty JAR file not found or invalid");
                return new long[]{};
            }
            
            DexFile dexFile = new DexFile(emptyJar);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            long[] longs = new long[cookies.size()];
            for (int i = 0; i < cookies.size(); i++) {
                longs[i] = cookies.get(i);
            }
            Log.d(TAG, "Successfully loaded empty DEX with " + cookies.size() + " cookies");
            return longs;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load empty DEX", e);
        }
        return new long[]{};
    }

    private static byte[] createMinimalDexBytes() {

        byte[] dexHeader = {
            'd', 'e', 'x', '\n',  
            0x30, 0x33, 0x35, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x70, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00   
        };
        
        return dexHeader;
    }
}
