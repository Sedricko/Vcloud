package com.vcloud.motor.fake.service;

import android.os.IBinder;
import android.os.LocaleList;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.vcloud.motor.fake.hook.BinderInvocationStub;
import com.vcloud.motor.fake.hook.MethodHook;
import com.vcloud.motor.fake.hook.ProxyMethod;

// Vcloud: o idioma por app (Android 13+) é guardado pelo pacote real; para um app de dentro o Android recusa
// (SecurityException CHANGE_CONFIGURATION / READ_APP_SPECIFIC_LOCALES) e a Play Store caía. Aqui o app de dentro
// usa o idioma do celular.
public class ILocaleManagerProxy extends BinderInvocationStub {
    private static final String SERVICO = "locale";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService(SERVICO));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder b = BRServiceManager.get().getService(SERVICO);
            return Class.forName("android.app.ILocaleManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, b);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICO);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return LocaleList.getEmptyLocaleList();
        }
    }
}
