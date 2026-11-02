package com.vcloud.motor.fake.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.app.BRAppOpsManager;
import black.android.os.BRServiceManager;
import black.com.android.internal.app.BRIAppOpsServiceStub;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.fake.hook.BinderInvocationStub;
import com.vcloud.motor.fake.hook.MethodHook;
import com.vcloud.motor.fake.hook.ProxyMethod;
import com.vcloud.motor.utils.MethodParameterUtils;
import com.vcloud.motor.utils.Slog;


public class IAppOpsManagerProxy extends BinderInvocationStub {
    public IAppOpsManagerProxy() {
        super(BRServiceManager.get().getService(Context.APP_OPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder call = BRServiceManager.get().getService(Context.APP_OPS_SERVICE);
        return BRIAppOpsServiceStub.get().asInterface(call);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRAppOpsManager.get(null)._check_mService() != null) {
            AppOpsManager appOpsManager = (AppOpsManager) VcloudCore.getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                BRAppOpsManager.get(appOpsManager)._set_mService(getProxyInvocation());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        replaceSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        // Vcloud: "sobrepor a outros apps" (op 24) responde o estado real do Vcloud (que é quem desenha a janela);
        // antes dizia sempre "permitido" e o Android recusava o balão do Unit (permission denied for window type 2038).
        if (args != null && args.length > 2 && args[0] instanceof Integer && (Integer) args[0] == 24
                && (methodName.startsWith("check") || methodName.startsWith("note"))) {
            int modo = modoSobreposicaoDoVcloud();
            Slog.i("VcloudSobrepor", methodName + " -> " + modo + " (0=permitido)");
            if (methodName.startsWith("checkOperation") || methodName.startsWith("checkOp")) {
                return modo;
            }
            if (modo != AppOpsManager.MODE_ALLOWED) {
                // noteOperation devolve tipos diferentes conforme o Android; com o modo negado, usa a resposta real
                args[1] = VcloudCore.getHostUid();
                if (args[2] instanceof String) args[2] = VcloudCore.getHostPkg();
                try { return method.invoke(getBase(), args); } catch (Throwable ignored) { }
            }
        }
        
        
        if (methodName.startsWith("check") || 
            methodName.startsWith("note") || 
            methodName.startsWith("start")) {
            Slog.d(TAG, "AppOps invoke: Bypassing system for " + methodName + ", allowing operation");
            return permitido(method, args);
        }
        
        
        if (methodName.startsWith("finish")) {
            Slog.d(TAG, "AppOps invoke: Bypassing system for " + methodName);
            return null;
        }
        
        
        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            
            Slog.w(TAG, "AppOps invoke: SecurityException caught for " + methodName + ", allowing operation", e);
            return AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Slog.e(TAG, "AppOps invoke: Error in method " + methodName, e);
            
            return AppOpsManager.MODE_ALLOWED;
        }
    }


    // Vcloud: note*/start* devolvem int (Android antigo) ou SyncNotedAppOp (Android 11+); devolver int onde se espera
    // SyncNotedAppOp dava ClassCastException no gms (startOperation).
    private static Object permitido(Method method, Object[] args) {
        Class<?> tipo = method.getReturnType();
        if (tipo == void.class) return null;
        if (tipo == int.class || tipo == Integer.class) return AppOpsManager.MODE_ALLOWED;
        if (tipo.getSimpleName().equals("SyncNotedAppOp")) {
            Integer op = null;
            String pkg = null;
            if (args != null) {
                for (Object a : args) {
                    if (op == null && a instanceof Integer) op = (Integer) a;
                    if (pkg == null && a instanceof String) pkg = (String) a;
                }
            }
            int codigo = op == null ? 0 : op;
            try {
                return tipo.getConstructor(int.class, int.class, String.class, String.class)
                        .newInstance(AppOpsManager.MODE_ALLOWED, codigo, null, pkg);
            } catch (Throwable ignored) { }
            try {
                return tipo.getConstructor(int.class, String.class, String.class).newInstance(codigo, null, pkg);
            } catch (Throwable e) {
                Slog.w(TAG, "não consegui criar SyncNotedAppOp: " + e);
            }
        }
        return null;
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Pergunta ao Android, com a identidade do Vcloud, se ele pode sobrepor; MODE_DEFAULT conta como a chave do sistema.
    private int modoSobreposicaoDoVcloud() {
        try {
            Method m = getBase().getClass().getMethod("checkOperation", int.class, int.class, String.class);
            int modo = (Integer) m.invoke(getBase(), 24, VcloudCore.getHostUid(), VcloudCore.getHostPkg());
            if (modo == AppOpsManager.MODE_DEFAULT) {
                modo = VcloudCore.getContext().checkPermission(android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                        android.os.Process.myPid(), VcloudCore.getHostUid()) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED;
            }
            return modo;
        } catch (Throwable e) {
            Slog.w("VcloudSobrepor", "não consegui consultar: " + e);
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            
            Slog.d(TAG, "AppOps CheckOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("checkOperationForDevice")
    public static class CheckOperationForDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps CheckOperationForDevice: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps NoteOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps CheckOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("startOp")
    public static class StartOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps StartOp: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("startOpNoThrow")
    public static class StartOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "AppOps StartOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("finishOp")
    public static class FinishOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && isMediaStorageOrAudioOp(name)) {
                    Slog.d(TAG, "AppOps FinishOp: Finishing operation: " + name);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    
    @ProxyMethod("noteOp")
    public static class NoteOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && (name.contains("RECORD_AUDIO") || name.contains("AUDIO") || name.contains("MICROPHONE"))) {
                    Slog.d(TAG, "AppOps NoteOp: Allowing RECORD_AUDIO operation: " + name);
                    return AppOpsManager.MODE_ALLOWED;
                }
            } catch (Throwable ignored) {
            }
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && (name.contains("RECORD_AUDIO") || name.contains("AUDIO") || name.contains("MICROPHONE"))) {
                    Slog.d(TAG, "AppOps NoteOpNoThrow: Allowing RECORD_AUDIO operation: " + name);
                    return AppOpsManager.MODE_ALLOWED;
                }
            } catch (Throwable ignored) {
            }
            return method.invoke(who, args);
        }
    }

    private static boolean isMediaStorageOrAudioOp(String opPublicNameOrStr) {
        if (opPublicNameOrStr == null) return false;
        
        String n = opPublicNameOrStr.toUpperCase();
        return n.contains("READ_MEDIA")
                || n.contains("READ_EXTERNAL_STORAGE")
                || n.contains("RECORD_AUDIO")
                || n.contains("CAPTURE_AUDIO_OUTPUT")
                || n.contains("MODIFY_AUDIO_SETTINGS")
                || n.contains("AUDIO")
                || n.contains("MICROPHONE")
                || n.contains("FOREGROUND_SERVICE")
                || n.contains("SYSTEM_ALERT_WINDOW")
                || n.contains("WRITE_SETTINGS")
                || n.contains("ACCESS_FINE_LOCATION")
                || n.contains("ACCESS_COARSE_LOCATION")
                || n.contains("CAMERA")
                || n.contains("BODY_SENSORS")
                || n.contains("BLUETOOTH_SCAN")
                || n.contains("BLUETOOTH_CONNECT")
                || n.contains("BLUETOOTH_ADVERTISE")
                || n.contains("NEARBY_WIFI_DEVICES")
                || n.contains("POST_NOTIFICATIONS");
    }

    private static String getOpPublicName(int op) {
        try {
            
            java.lang.reflect.Method m = AppOpsManager.class.getMethod("opToPublicName", int.class);
            Object name = m.invoke(null, op);
            return name != null ? name.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
