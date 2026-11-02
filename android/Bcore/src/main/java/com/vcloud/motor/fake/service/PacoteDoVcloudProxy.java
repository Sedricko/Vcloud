package com.vcloud.motor.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.fake.hook.BinderInvocationStub;
import com.vcloud.motor.utils.Slog;

/**
 * Vcloud: serviços do sistema que conferem se o nome do pacote da chamada é do uid de quem chama (ex.: biometria:
 * "com.google.android.gms from uid 10454 not allowed to perform USE_BIOMETRIC"). Em todas as chamadas, o nome do app de dentro
 * é trocado pelo do Vcloud (que é de fato o dono do uid).
 */
public abstract class PacoteDoVcloudProxy extends BinderInvocationStub {
    private final String servico, stub;

    protected PacoteDoVcloudProxy(String servico, String stub) {
        super(BRServiceManager.get().getService(servico));
        this.servico = servico;
        this.stub = stub;
    }

    @Override
    protected Object getWho() {
        IBinder b = BRServiceManager.get().getService(servico);
        if (b == null) return null;
        try {
            return Class.forName(stub).getMethod("asInterface", IBinder.class).invoke(null, b);
        } catch (Throwable e) {
            Slog.e("PacoteDoVcloudProxy", "serviço " + servico + " indisponível", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(servico);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String guest = BActivityThread.getAppPackageName();
        if (args != null && guest != null) {
            for (int i = 0; i < args.length; i++) if (guest.equals(args[i])) args[i] = VcloudCore.getHostPkg();
        }
        return super.invoke(proxy, method, args);
    }
}
