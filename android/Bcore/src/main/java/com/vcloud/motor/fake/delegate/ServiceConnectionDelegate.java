package com.vcloud.motor.fake.delegate;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

import black.android.app.BRIServiceConnectionO;
import com.vcloud.motor.utils.compat.BuildCompat;


public class ServiceConnectionDelegate extends IServiceConnection.Stub {
    private static final Map<IBinder, ServiceConnectionDelegate> sServiceConnectDelegate = new HashMap<>();
    private final IServiceConnection mConn;
    private final ComponentName mComponentName;

    private ServiceConnectionDelegate(IServiceConnection mConn, ComponentName targetComponent) {
        this.mConn = mConn;
        this.mComponentName = targetComponent;
    }

    public static ServiceConnectionDelegate getDelegate(IBinder iBinder) {
        return sServiceConnectDelegate.get(iBinder);
    }

    public static IServiceConnection createProxy(IServiceConnection base, Intent intent) {
        final IBinder iBinder = base.asBinder();
        ServiceConnectionDelegate delegate = sServiceConnectDelegate.get(iBinder);
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        sServiceConnectDelegate.remove(iBinder);
                        iBinder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new ServiceConnectionDelegate(base, intent.getComponent());
            sServiceConnectDelegate.put(iBinder, delegate);
        }
        return delegate;
    }

    @Override
    public void connected(ComponentName name, IBinder service) throws RemoteException {
        connected(name, service, false);
    }

    /** Android 17 (API 37): o sistema chama esta versão com IBinderSession. Sem ela o app fecha (AbstractMethodError). */
    public void connected(ComponentName name, IBinder service, android.app.IBinderSession session, boolean dead) throws RemoteException {
        try {
            java.lang.reflect.Method m = mConn.getClass().getMethod("connected",
                    ComponentName.class, IBinder.class, android.app.IBinderSession.class, boolean.class);
            m.invoke(mConn, mComponentName, service, session, dead);
        } catch (NoSuchMethodException e) {
            connected(name, service, dead);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable c = e.getCause();
            if (c instanceof RemoteException) throw (RemoteException) c;
            throw new RuntimeException(c);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void connected(ComponentName name, IBinder service, boolean dead) throws RemoteException {
        if (BuildCompat.isOreo()) {
            BRIServiceConnectionO.get(mConn).connected(mComponentName, service, dead);
        } else {
            mConn.connected(name, service);
        }
    }
}
