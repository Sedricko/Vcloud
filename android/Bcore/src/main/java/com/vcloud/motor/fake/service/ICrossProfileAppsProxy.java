package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import com.vcloud.motor.utils.Diagnostico;
import com.vcloud.motor.utils.ValorPadrao;

/**
 * Serviço "crossprofileapps": a Play Store pergunta os perfis de destino (getTargetUserProfiles) ao tocar no ícone do perfil e o Android
 * recusa o nome do pacote do app de dentro ("Package com.android.vending does not belong to 10454"), o que derrubava a loja. O espaço
 * virtual não tem perfis de trabalho: o nome vai como o do Vcloud e uma recusa vira lista vazia.
 */
public class ICrossProfileAppsProxy extends PacoteDoVcloudProxy {
    public ICrossProfileAppsProxy() { super("crossprofileapps", "android.content.pm.ICrossProfileApps$Stub"); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            Diagnostico.eventoLimitado("crossprofile|" + method.getName(), 300_000,
                    "PERFIS " + method.getName() + " recusado pelo Android (" + e.getMessage() + "); resposta: vazia");
            return ValorPadrao.de(method.getReturnType());
        }
    }
}
