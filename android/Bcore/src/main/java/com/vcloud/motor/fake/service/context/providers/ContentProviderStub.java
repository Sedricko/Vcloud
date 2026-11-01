package com.vcloud.motor.fake.service.context.providers;

import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.content.BRAttributionSource;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.fake.hook.ClassInvocationStub;
import com.vcloud.motor.utils.compat.ContextCompat;
import com.vcloud.motor.utils.Slog;
import android.os.Bundle;
import com.vcloud.motor.utils.AttributionSourceUtils;

public class ContentProviderStub extends ClassInvocationStub implements BContentProvider {
    public static final String TAG = "ContentProviderStub";
    private IInterface mBase;
    private String mAppPkg;

    public IInterface wrapper(final IInterface contentProviderProxy, final String appPkg) {
        mBase = contentProviderProxy;
        mAppPkg = appPkg;
        injectHook();
        return (IInterface) getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {

    }

    @Override
    protected void onBindMethod() {

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("asBinder".equals(method.getName())) {
            return method.invoke(mBase, args);
        }

        String methodName = method.getName();

        if ("call".equals(methodName)) {
            
            fixarOrigem(args);
        } else {
            
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof String) {
                        String strArg = (String) arg;
                        
                        if (!isSystemProviderAuthority(strArg)) {
                            
                            args[i] = mAppPkg;
                        }
                    }
                }
                
                fixarOrigem(args);
            }
        }

        methodName = method.getName();
        if (methodName.equals("query") || methodName.equals("insert") || 
            methodName.equals("update") || methodName.equals("delete") || 
            methodName.equals("bulkInsert") || methodName.equals("call")) {

            try {
                return method.invoke(mBase, args);
            } catch (Throwable e) {
                
                Throwable cause = e.getCause();
                if (isUidMismatchError(cause)) {
                    Slog.w(TAG, "UID mismatch in ContentProvider call, returning safe default: " + cause.getMessage());
                    com.vcloud.motor.utils.Diagnostico.eventoLimitado("prov|" + methodName + "|" + mAppPkg, 60_000,
                            "PROVEDOR erro engolido (" + methodName + ") em " + mAppPkg + ": " + com.vcloud.motor.utils.CausaDeErro.resumirMensagem(cause.getMessage(), 140));
                    return getSafeDefaultValue(methodName, method.getReturnType());
                } else if (cause instanceof RuntimeException) {
                    String message = cause.getMessage();
                    if (message != null && (message.contains("uid") || message.contains("permission"))) {
                        Slog.w(TAG, "Permission/UID error in ContentProvider call, returning safe default: " + message);
                        com.vcloud.motor.utils.Diagnostico.eventoLimitado("prov|" + methodName + "|" + mAppPkg, 60_000,
                                "PROVEDOR erro engolido (" + methodName + ") em " + mAppPkg + ": " + com.vcloud.motor.utils.CausaDeErro.resumirMensagem(message, 140));
                        return getSafeDefaultValue(methodName, method.getReturnType());
                    }
                }

                if (methodName.equals("call")) {
                    Slog.w(TAG, "Error in call method, returning safe default: " + e.getMessage());
                    return getSafeDefaultValue(methodName, method.getReturnType());
                }
                
                throw e.getCause();
            }
        }

        try {
            return method.invoke(mBase, args);
        } catch (Throwable e) {
            
            Throwable cause = e.getCause();
            if (isUidMismatchError(cause)) {
                Slog.w(TAG, "UID mismatch in " + methodName + ", returning safe default: " + cause.getMessage());
                return getSafeDefaultValue(methodName, method.getReturnType());
            }
            throw e.getCause();
        }
    }
    
    // Vcloud: o provedor de dentro compara o uid de quem chama (virtual, pelo gancho do Binder) com o uid da origem;
    // o antigo ajuste punha o uid do celular e dava "Calling uid doesn't match source uid".
    private static void fixarOrigem(Object[] args) {
        AttributionSourceUtils.fixAttributionSourceInArgs(args);
        if (args == null) return;
        int uid = BActivityThread.getBUid();
        for (Object a : args) {
            if (a != null && a.getClass().getName().endsWith("AttributionSource")) {
                try { com.vcloud.motor.utils.compat.ContextCompat.fixAttributionSourceState(a, uid); } catch (Throwable ignored) {}
            }
        }
    }

    private Object getSafeDefaultValue(String methodName) {
        switch (methodName) {
            case "query":
                return null; 
            case "insert":
                return null; 
            case "update":
            case "delete":
                return 0; 
            case "bulkInsert":
                return 0; 
            case "call":
                return new Bundle(); 
            case "getType":
                return null; 
            case "openFile":
                return null; 
            case "openAssetFile":
                return null; 
            default:
                return null; 
        }
    }

    private boolean isSystemProviderAuthority(String authority) {
        if (authority == null) return false;

        return authority.equals("settings") || 
               authority.equals("settings_global") || 
               authority.equals("settings_system") || 
               authority.equals("settings_secure") ||
               authority.equals("media") ||
               authority.equals("telephony") ||
               authority.startsWith("android.provider.Settings");
    }

    private boolean isUidMismatchError(Throwable error) {
        if (error == null) return false;
        
        String message = error.getMessage();
        if (message == null) return false;

        return message.contains("Calling uid") && 
               message.contains("doesn't match source uid") ||
               message.contains("uid") && 
               message.contains("permission") ||
               message.contains("SecurityException") ||
               message.contains("UID mismatch");
    }

    private Object getSafeDefaultValue(String methodName, Class<?> returnType) {
        if (returnType == null) {
            return getSafeDefaultValue(methodName);
        }

        if (returnType == String.class) {
            return "true"; 
        } else if (returnType == int.class || returnType == Integer.class) {
            return 1; 
        } else if (returnType == long.class || returnType == Long.class) {
            return 1L; 
        } else if (returnType == float.class || returnType == Float.class) {
            return 1.0f; 
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return true; 
        } else if (returnType == Bundle.class) {
            return new Bundle(); 
        }

        return getSafeDefaultValue(methodName);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
