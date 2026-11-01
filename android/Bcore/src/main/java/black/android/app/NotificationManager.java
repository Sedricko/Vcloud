package black.android.app;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.app.NotificationManager")
public interface NotificationManager {
    @BStaticField
    IInterface sService();

    @BStaticMethod
    IInterface getService();
}
