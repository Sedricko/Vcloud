package black.android.os;

import android.os.IBinder;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.os.Bundle")
public interface Bundle {
    @BMethod
    IBinder getIBinder(String String0);

    @BMethod
    void putIBinder(String String0, IBinder IBinder1);
}
