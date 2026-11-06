package com.vcloud.motor.fake.delegate;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import black.android.content.BRIIntentReceiver;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.proxy.record.ProxyBroadcastRecord;


public class InnerReceiverDelegate extends IIntentReceiver.Stub {
    public static final String TAG = "InnerReceiverDelegate";

    private static final Map<IBinder, InnerReceiverDelegate> sInnerReceiverDelegate = new HashMap<>();
    private final WeakReference<IIntentReceiver> mIntentReceiver;
    private final java.util.List<android.content.IntentFilter> mFiltros = new java.util.concurrent.CopyOnWriteArrayList<>();

    private InnerReceiverDelegate(IIntentReceiver iIntentReceiver) {
        this.mIntentReceiver = new WeakReference<>(iIntentReceiver);
    }

    /** Guarda o filtro com que o app registrou o receptor, para poder avisá-lo de mudanças de pacote do Vcloud. */
    public static void lembrarFiltro(IIntentReceiver proxy, Object[] args) {
        if (!(proxy instanceof InnerReceiverDelegate)) return;
        for (Object a : args) {
            if (a instanceof android.content.IntentFilter) ((InnerReceiverDelegate) proxy).mFiltros.add((android.content.IntentFilter) a);
        }
    }

    /** Entrega (só neste processo) um aviso de pacote aos receptores registrados cujo filtro combina. O sistema real não manda PACKAGE_ADDED de app de dentro. */
    public static void entregar(Intent intent) {
        java.util.List<InnerReceiverDelegate> todos;
        synchronized (sInnerReceiverDelegate) { todos = new java.util.ArrayList<>(sInnerReceiverDelegate.values()); }
        for (InnerReceiverDelegate d : todos) {
            boolean combina = false;
            for (android.content.IntentFilter f : d.mFiltros) {
                try { if (f.match(null, intent, false, TAG) >= 0) { combina = true; break; } } catch (Throwable ignored) { }
            }
            if (!combina) continue;
            try { d.performReceive(new Intent(intent), 0, null, null, false, false, 0); } catch (Throwable t) { t.printStackTrace(); }
        }
    }

    public static InnerReceiverDelegate getDelegate(IBinder iBinder) {
        synchronized (sInnerReceiverDelegate) { return sInnerReceiverDelegate.get(iBinder); }
    }

    public static IIntentReceiver createProxy(IIntentReceiver base) {
        if (base instanceof InnerReceiverDelegate) {
            return base;
        }
        final IBinder iBinder = base.asBinder();
        InnerReceiverDelegate delegate;
        synchronized (sInnerReceiverDelegate) { delegate = sInnerReceiverDelegate.get(iBinder); }
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        synchronized (sInnerReceiverDelegate) { sInnerReceiverDelegate.remove(iBinder); }
                        iBinder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new InnerReceiverDelegate(base);
            synchronized (sInnerReceiverDelegate) { sInnerReceiverDelegate.put(iBinder, delegate); }
        }
        return delegate;
    }

    @Override
    public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        intent.setExtrasClassLoader(VcloudCore.getApplication().getClassLoader());
        ProxyBroadcastRecord proxyBroadcastRecord = ProxyBroadcastRecord.create(intent);
        Intent perIntent;
        if (proxyBroadcastRecord.mIntent != null) {
            proxyBroadcastRecord.mIntent.setExtrasClassLoader(VcloudCore.getApplication().getClassLoader());
            perIntent = proxyBroadcastRecord.mIntent;
        } else {
            perIntent = intent;
        }
        IIntentReceiver iIntentReceiver = mIntentReceiver.get();
        if (iIntentReceiver != null) {
            BRIIntentReceiver.get(iIntentReceiver).performReceive(perIntent, resultCode, data, extras, ordered, sticky, sendingUser);
        }
    }
}
