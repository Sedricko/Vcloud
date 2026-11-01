package black.android.app;

import android.app.Notification;
import android.content.Context;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.app.Notification")
public interface NotificationL {
    @BClassName("android.app.Notification$Builder")
    interface Builder {
        @BStaticMethod
        Notification rebuild(Context Context0, Notification Notification1);
    }
}
