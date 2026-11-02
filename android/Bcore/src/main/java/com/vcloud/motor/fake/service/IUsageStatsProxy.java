package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import com.vcloud.motor.utils.Diagnostico;
import com.vcloud.motor.utils.ValorPadrao;

/**
 * Serviço "usagestats": a Play Store consulta o uso de apps (queryEvents) e o Android recusa porque o Vcloud não tem o acesso
 * "uso de apps" ("com.android.vending from uid 10454 not allowed to perform GET_USAGE_STATS"). Recusa vira resposta vazia.
 */
public class IUsageStatsProxy extends PacoteDoVcloudProxy {
    public IUsageStatsProxy() { super("usagestats", "android.app.usage.IUsageStatsManager$Stub"); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            Diagnostico.eventoLimitado("usagestats|" + method.getName(), 300_000,
                    "USO-DE-APPS " + method.getName() + " recusado pelo Android; resposta: vazia");
            return ValorPadrao.de(method.getReturnType());
        }
    }
}
