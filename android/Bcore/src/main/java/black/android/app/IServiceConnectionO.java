package black.android.app;

import android.content.ComponentName;
import android.os.IBinder;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.app.IServiceConnection")
public interface IServiceConnectionO {
    @BMethod
    void connected(ComponentName ComponentName0, IBinder IBinder1, boolean boolean2);
}
