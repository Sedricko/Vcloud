package black.com.android.internal.telephony;

import android.os.IBinder;
import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("com.android.internal.telephony.IMms")
public interface IMms {
    @BClassName("com.android.internal.telephony.IMms$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
