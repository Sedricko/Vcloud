package com.vcloud.motor.fake.service;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.ArrayList;

import black.android.content.pm.BRUserInfo;
import black.android.os.BRIUserManagerStub;
import black.android.os.BRServiceManager;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.fake.hook.BinderInvocationStub;
import com.vcloud.motor.fake.hook.MethodHook;
import com.vcloud.motor.fake.hook.ProxyMethod;


public class IUserManagerProxy extends BinderInvocationStub {
    public IUserManagerProxy() {
        super(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIUserManagerStub.get().asInterface(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            args[0] = VcloudCore.getHostPkg();
            return method.invoke(who, args);
        }
    }

    /**
     * Android 17: UserManager.getApplicationRestrictions chama getApplicationRestrictionsForUser(pacote, userId) e o sistema só
     * aceita se o pacote for do próprio uid de quem chama (senão "Only system may: get application restrictions for other
     * user/app" e o addAccount do Google falha). O gancho acima tem o nome antigo e nunca pegava essa chamada.
     */
    @ProxyMethod("getApplicationRestrictionsForUser")
    public static class GetApplicationRestrictionsForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            args[0] = VcloudCore.getHostPkg();
            if (args.length > 1 && args[1] instanceof Integer) args[1] = VcloudCore.getHostUserId();
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProfileParent")
    public static class GetProfileParent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object blackBox = BRUserInfo.get()._new(BActivityThread.getUserId(), "Vcloud", BRUserInfo.get().FLAG_PRIMARY());
            return blackBox;
        }
    }

    @ProxyMethod("getUsers")
    public static class getUsers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new ArrayList<>();
        }
    }
}
