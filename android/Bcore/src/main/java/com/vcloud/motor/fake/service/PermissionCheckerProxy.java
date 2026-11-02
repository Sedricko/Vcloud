package com.vcloud.motor.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.vcloud.motor.fake.hook.BinderInvocationStub;
import com.vcloud.motor.fake.hook.MethodHook;
import com.vcloud.motor.fake.hook.ProxyMethod;
import com.vcloud.motor.utils.Slog;

/**
 * Vcloud: no Android 17 o autenticador do Google (de dentro) confere ACCOUNT_MANAGER pelo PermissionEnforcer/PermissionChecker,
 * e o uid de quem chama é o do Vcloud (sem essa permissão de sistema). Responde "concedida" só para ela, como o gancho do AMS já faz.
 */
public class PermissionCheckerProxy extends BinderInvocationStub {
    public static final String TAG = "PermissionCheckerProxy";
    private static final String SERVICE = "permission_checker";

    public PermissionCheckerProxy() {
        super(BRServiceManager.get().getService(SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(SERVICE);
        if (binder == null) return null;
        try {
            Class<?> stub = Class.forName("android.permission.IPermissionChecker$Stub");
            return stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        } catch (Throwable e) {
            Slog.e(TAG, "IPermissionChecker indisponível", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("checkPermission")
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && android.Manifest.permission.ACCOUNT_MANAGER.equals(args[0])) {
                com.vcloud.motor.utils.Diagnostico.eventoLimitado("permchk", 60_000, "LOGIN permissão ACCOUNT_MANAGER concedida (permission_checker)");
                return 0; // PERMISSION_GRANTED
            }
            return method.invoke(who, args);
        }
    }
}
