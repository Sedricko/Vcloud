package black.android.app;

import android.os.IBinder;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.app.ClientTransactionHandler")
public interface ClientTransactionHandler {
    @BMethod
    Object getActivityClient(IBinder IBinder0);
}
