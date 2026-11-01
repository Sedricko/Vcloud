package black.android.app.role;

import android.os.IBinder;
import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.app.role.IRoleManager")
public interface IRoleManager {
    @BClassName("android.app.role.IRoleManager$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
