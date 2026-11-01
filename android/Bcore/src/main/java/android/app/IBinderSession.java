package android.app;

import android.os.IInterface;

/**
 * Vcloud: só para compilar. No Android 17 (API 37) o sistema passou a chamar
 * IServiceConnection.connected(ComponentName, IBinder, IBinderSession, boolean);
 * em tempo de execução vale a classe real do sistema.
 */
public interface IBinderSession extends IInterface {
}
