package black.android.app.servertransaction;

import android.os.IBinder;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.servertransaction.ClientTransaction")
public interface ClientTransaction {
    @BField
    List<Object> mActivityCallbacks();

    @BField
    IBinder mActivityToken();

    @BField
    Object mLifecycleStateRequest();
}
