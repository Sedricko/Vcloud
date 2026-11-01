package black.android.app;

import android.os.IBinder;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.app.ActivityThread")
public interface ActivityThreadQ {
    @BMethod
    void handleNewIntent(IBinder IBinder0, List List1);
}
