package black.android.nfc;

import android.os.IBinder;
import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.nfc.INfcAdapter")
public interface INfcAdapter {
    @BClassName("android.nfc.INfcAdapter$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
