package black.android.app;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.app.ActivityManagerNative")
public interface ActivityManagerNative {
    @BStaticField
    Object gDefault();

    @BStaticMethod
    IInterface getDefault();
}
