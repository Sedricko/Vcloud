package com.vcloud.motor.fake.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import java.lang.reflect.Field;

import black.android.app.BRActivity;
import black.android.app.BRActivityThread;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.fake.hook.HookManager;
import com.vcloud.motor.fake.hook.IInjectHook;
import com.vcloud.motor.fake.service.HCallbackProxy;
import com.vcloud.motor.fake.service.IActivityClientProxy;
import com.vcloud.motor.utils.HackAppUtils;
import com.vcloud.motor.utils.compat.ActivityCompat;
import com.vcloud.motor.utils.compat.ActivityManagerCompat;
import com.vcloud.motor.utils.compat.ContextCompat;

public final class AppInstrumentation extends BaseInstrumentationDelegate implements IInjectHook {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation sAppInstrumentation;

    public static AppInstrumentation get() {
        if (sAppInstrumentation == null) {
            synchronized (AppInstrumentation.class) {
                if (sAppInstrumentation == null) {
                    sAppInstrumentation = new AppInstrumentation();
                }
            }
        }
        return sAppInstrumentation;
    }

    public AppInstrumentation() {
    }

    @Override
    public void injectHook() {
        try {
            Instrumentation mInstrumentation = getCurrInstrumentation();
            if (mInstrumentation == this || checkInstrumentation(mInstrumentation))
                return;
            mBaseInstrumentation = (Instrumentation) mInstrumentation;
            BRActivityThread.get(VcloudCore.mainThread())._set_mInstrumentation(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instrumentation getCurrInstrumentation() {
        Object currentActivityThread = VcloudCore.mainThread();
        return BRActivityThread.get(currentActivityThread).mInstrumentation();
    }

    @Override
    public boolean isBadEnv() {
        return !checkInstrumentation(getCurrInstrumentation());
    }

    private boolean checkInstrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof AppInstrumentation) {
            return true;
        }
        Class<?> clazz = instrumentation.getClass();
        if (Instrumentation.class.equals(clazz)) {
            return false;
        }
        do {
            assert clazz != null;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Instrumentation.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Object obj = field.get(instrumentation);
                        if ((obj instanceof AppInstrumentation)) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (!Instrumentation.class.equals(clazz));
        return false;
    }

    private void checkHCallback() {
        HookManager.get().checkEnv(HCallbackProxy.class);
    }

    private void checkActivity(Activity activity) {
        Log.d(TAG, "callActivityOnCreate: " + activity.getClass().getName());
        HackAppUtils.enableQQLogOutput(activity.getPackageName(), activity.getClassLoader());
        checkHCallback();
        HookManager.get().checkEnv(IActivityClientProxy.class);
        ActivityInfo info = BRActivity.get(activity).mActivityInfo();
        ContextCompat.fix(activity);
        ActivityCompat.fix(activity);
        if (info.theme != 0) {
            activity.getTheme().applyStyle(info.theme, true);
        }
        int orientacao = info.screenOrientation;
        if (orientacao == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Vcloud: o motor perde a orientação de alguns jogos (ex.: DYSMANTLE pedia deitado e chegava -1).
            // Lê direto do APK do jogo o que a tela pede.
            orientacao = orientacaoDoApk(activity, info.name);
        }
        Log.i(TAG, "Orientação: " + activity.getClass().getName() + " pede screenOrientation=" + info.screenOrientation
                + " usada=" + orientacao);
        ActivityManagerCompat.setActivityOrientation(activity, orientacao);
    }

    private static final java.util.Map<String, Integer> sOrientacaoDoApk = new java.util.concurrent.ConcurrentHashMap<>();

    private static int orientacaoDoApk(Activity activity, String nomeTela) {
        if (nomeTela == null) nomeTela = activity.getClass().getName();
        Integer guardada = sOrientacaoDoApk.get(nomeTela);
        if (guardada != null) return guardada;
        int resultado = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        try {
            String apk = activity.getApplicationInfo().sourceDir;
            android.content.pm.PackageInfo pi = activity.getPackageManager()
                    .getPackageArchiveInfo(apk, android.content.pm.PackageManager.GET_ACTIVITIES);
            if (pi != null && pi.activities != null) {
                for (ActivityInfo a : pi.activities) {
                    if (nomeTela.equals(a.name)) {
                        resultado = a.screenOrientation;
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "Não consegui ler a orientação do APK: " + e);
        }
        sOrientacaoDoApk.put(nomeTela, resultado);
        return resultado;
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ContextCompat.fix(context);

        return super.newApplication(cl, className, context);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        checkActivity(activity);
        super.callActivityOnCreate(activity, icicle, persistentState);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        checkActivity(activity);
        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        checkHCallback();
        super.callApplicationOnCreate(app);
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.newActivity(cl, className, intent);
        } catch (ClassNotFoundException e) {
            return mBaseInstrumentation.newActivity(cl, className, intent);
        }
    }
}
