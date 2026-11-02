package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import black.android.telephony.BRTelephonyManager;
import com.vcloud.motor.fake.hook.ClassInvocationStub;
import com.vcloud.motor.fake.hook.MethodHook;
import com.vcloud.motor.fake.hook.ProxyMethod;
import com.vcloud.motor.utils.MethodParameterUtils;


public class IPhoneSubInfoProxy extends ClassInvocationStub {
    public static final String TAG = "IPhoneSubInfoProxy";

    public IPhoneSubInfoProxy() {
        if (BRTelephonyManager.get()._check_sServiceHandleCacheEnabled() != null) {
            BRTelephonyManager.get()._set_sServiceHandleCacheEnabled(true);
        }
        if (BRTelephonyManager.get()._check_getSubscriberInfoService() != null) {
            BRTelephonyManager.get().getSubscriberInfoService();
        }
    }

    @Override
    protected Object getWho() {
        return BRTelephonyManager.get().sIPhoneSubInfo();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRTelephonyManager.get()._set_sIPhoneSubInfo(proxyInvocation);
    }

    /**
     * Identificadores do aparelho (chip/ICCID, IMEI, assinante...) só são dados a apps privilegiados (Android 10+). O Vcloud não é:
     * o checkin do Google de dentro caía com "getIccSerialNumber: The uid 10454 does not meet the requirements to access device
     * identifiers". O espaço virtual não expõe esses identificadores: a resposta é "sem valor" (null), sem derrubar o app.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            String m = e.getMessage();
            if (m == null || !(m.contains("device identifiers") || m.contains("READ_PRIVILEGED_PHONE_STATE") || m.contains("READ_PHONE_STATE"))) throw e;
            com.vcloud.motor.utils.Diagnostico.eventoLimitado("phone|" + method.getName(), 300_000,
                    "TELEFONE " + method.getName() + " recusado pelo Android (identificador do aparelho); resposta: sem valor");
            return com.vcloud.motor.utils.ValorPadrao.de(method.getReturnType());
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }


    @ProxyMethod("getLine1NumberForSubscriber")
    public static class getLine1NumberForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
