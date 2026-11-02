package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import com.vcloud.motor.utils.Diagnostico;
import com.vcloud.motor.utils.ValorPadrao;

/**
 * Serviço de backup do Android (serviço "backup"): perguntas como isBackupServiceActive exigem a permissão BACKUP (de sistema) e
 * derrubavam a tela de configuração do Google depois do login ("Neither user 10454 nor current process has android.permission.BACKUP").
 * O espaço virtual não tem backup do sistema: recusa vira resposta neutra (falso/vazio).
 */
public class IBackupManagerProxy extends PacoteDoVcloudProxy {
    public IBackupManagerProxy() { super("backup", "android.app.backup.IBackupManager$Stub"); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            Diagnostico.eventoLimitado("backup|" + method.getName(), 300_000,
                    "BACKUP " + method.getName() + " recusado pelo Android (permissão de sistema); resposta: neutra");
            return ValorPadrao.de(method.getReturnType());
        }
    }
}
