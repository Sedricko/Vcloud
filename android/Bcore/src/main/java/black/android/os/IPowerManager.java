package black.android.os;

import android.os.IBinder;
import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.os.IPowerManager")
public interface IPowerManager {
    @BClassName("android.os.IPowerManager$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
