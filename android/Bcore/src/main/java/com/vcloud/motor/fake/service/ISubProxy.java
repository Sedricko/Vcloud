package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import com.vcloud.motor.utils.Diagnostico;
import com.vcloud.motor.utils.ValorPadrao;

/**
 * Assinaturas do chip (serviço "isub"). Várias perguntas (getActiveSubIdList, ...) exigem READ_PRIVILEGED_PHONE_STATE, que só apps
 * privilegiados têm; o checkin do Google de dentro caía com "getActiveSubIdList. Does not have any of the following permissions".
 * O espaço virtual não expõe o chip: recusa vira resposta neutra (lista vazia / falso / zero).
 */
public class ISubProxy extends PacoteDoVcloudProxy {
    public ISubProxy() { super("isub", "com.android.internal.telephony.ISub$Stub"); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Diagnostico.eventoLimitado("tel|isub|" + method.getName(), 120_000, "TELEFONE consulta isub." + method.getName());
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            String m = e.getMessage();
            if (m == null || !(m.contains("PHONE_STATE") || m.contains("permission") || m.contains("identifiers"))) throw e;
            Diagnostico.eventoLimitado("isub|" + method.getName(), 300_000,
                    "CHIP " + method.getName() + " recusado pelo Android (permissão privilegiada); resposta: neutra");
            return ValorPadrao.de(method.getReturnType());
        }
    }
}
