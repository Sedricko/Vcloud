// IBActivityThread.aidl
package com.vcloud.motor.core;

// Declare any non-default types here with import statements

import android.os.IBinder;
import android.content.ComponentName;
import android.content.Intent;
import java.util.List;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import com.vcloud.motor.entity.am.ReceiverData;

interface IBActivityThread {
    IBinder getActivityThread();
    void bindApplication();
    void restartJobService(String selfId);
    IBinder acquireContentProviderClient(in ProviderInfo providerInfo);

    IBinder peekService(in Intent intent);
    void stopService(in Intent componentName);

    void finishActivity(IBinder token);
    void handleNewIntent(IBinder token, in Intent intent);

    void scheduleReceiver(in ReceiverData data);

    // Vcloud: avisa os receptores dinâmicos deste app que um app foi instalado/atualizado/removido dentro do Vcloud (PACKAGE_ADDED/REPLACED/REMOVED)
    oneway void mudancaDePacote(in Intent intent);

    // Vcloud: comando do painel lateral para a tela que está na frente deste app (ver ComandoDoPainel)
    void executarComando(int comando);
}
