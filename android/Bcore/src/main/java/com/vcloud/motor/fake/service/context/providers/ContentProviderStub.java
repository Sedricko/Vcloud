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
    
    // Vcloud: a origem leva sempre o uid do celular; do lado do provedor, NativeCore.getCallingUid devolve o uid do celular
    // durante a checagem da origem (o "uid de quem chamou" guardado no processo fica velho e dava "Calling uid doesn't match").
    private void fixarOrigem(Object[] args) {
        AttributionSourceUtils.fixAttributionSourceInArgs(args);
        if (args == null) return;
        // uid do celular (o Android confere), mas o NOME é o do app de dentro que pede: o Gservices só entrega/grava o
        // identificador do aparelho para pacotes aprovados ("Package com.vcloud.app is not approved to access the device ID").
        String quem = BActivityThread.getAppPackageName();
        if (quem == null || VcloudCore.getHostPkg().equals(mAppPkg)) quem = VcloudCore.getHostPkg();
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a == null || !a.getClass().getName().endsWith("AttributionSource")) continue;
            String antes = nomeDaOrigem(a);
            try { com.vcloud.motor.utils.compat.ContextCompat.fixAttributionSourceState(a, VcloudCore.getHostUid(), quem); } catch (Throwable ignored) {}
            String depois = nomeDaOrigem(a);
            if (!quem.equals(depois)) {
                // o ajuste no lugar não pegou (campo mudou no Android): cria uma origem nova com uid do celular e o nome certo
                Object nova = novaOrigem(a.getClass(), VcloudCore.getHostUid(), quem);
                if (nova != null) { args[i] = nova; depois = nomeDaOrigem(nova) + " (origem nova)"; }
            }
            com.vcloud.motor.utils.Diagnostico.eventoLimitado("origem|" + mAppPkg + "|" + quem, 120_000,
                    "ORIGEM do pedido ao provedor de " + mAppPkg + ": nome antes=" + antes + " depois=" + depois + " (quer=" + quem + ")");
        }
    }

    private static String nomeDaOrigem(Object a) {
        try { return (String) a.getClass().getMethod("getPackageName").invoke(a); } catch (Throwable e) { return "?"; }
    }

    private static Object novaOrigem(Class<?> tipo, int uid, String pacote) {
        try {
            java.lang.reflect.Constructor<?> c3 = tipo.getDeclaredConstructor(int.class, String.class, String.class);
            c3.setAccessible(true);
            return c3.newInstance(uid, pacote, null);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Constructor<?> c = tipo.getDeclaredConstructor(int.class, String.class);
                c.setAccessible(true);
                return c.newInstance(uid, pacote);
            } catch (Throwable e2) { return null; }
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
