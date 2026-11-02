package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import com.vcloud.motor.utils.Diagnostico;
import com.vcloud.motor.utils.Reflector;

/**
 * BiometricPrompt (serviço "auth").
 * O sistema só exige a permissão SET_BIOMETRIC_DIALOG_ADVANCED (que o Vcloud não tem) quando o pedido traz logo, descrição do logo ou
 * conteúdo personalizado (PromptInfo.requiresAdvancedPermission, lido no código do Android). Esses itens são só decoração da janela:
 * saem do pedido e a confirmação (tela de bloqueio/biometria) segue igual.
 */
public class IAuthServiceProxy extends PacoteDoVcloudProxy {
    public IAuthServiceProxy() { super("auth", "android.hardware.biometrics.IAuthService$Stub"); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null && method.getName().equals("authenticate")) {
            for (Object a : args) {
                if (a != null && a.getClass().getName().equals("android.hardware.biometrics.PromptInfo")) limpar(a);
            }
        }
        return super.invoke(proxy, method, args);
    }

    private static void limpar(Object promptInfo) {
        boolean mexeu = false;
        String[][] campos = {{"mLogoRes", "0"}, {"mLogoBitmap", null}, {"mLogoDescription", null}, {"mContentView", null}};
        for (String[] c : campos) {
            try {
                Reflector r = Reflector.with(promptInfo);
                if (c[1] != null) r.field(c[0]).set(0); else r.field(c[0]).set(null);
                mexeu = true;
            } catch (Throwable e) {
                Diagnostico.eventoLimitado("bio|" + c[0], 300_000, "BIOMETRIA não consegui limpar " + c[0] + ": " + e);
            }
        }
        if (mexeu) Diagnostico.eventoLimitado("bio", 300_000, "BIOMETRIA recursos avançados (logo/conteúdo) tirados do pedido");
    }
}
