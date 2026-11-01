package black.android.app;


import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.os.IBinder;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;
import com.vcloud.reflexao.annotation.BParamClass;

@BClassName("android.app.Service")
public interface Service {
    @BMethod
    void attach(Context context,
                @BParamClass(ActivityThread.class) Object thread, String className, IBinder token,
                Application application, Object activityManager);
}
