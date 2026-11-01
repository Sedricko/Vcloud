package black.android.app;

import android.content.Intent;
import android.os.IBinder;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.app.PendingIntent")
public interface PendingIntentJBMR2 {
    @BConstructor
    PendingIntentJBMR2 _new(IBinder IBinder0);

    @BMethod
    Intent getIntent();
}
