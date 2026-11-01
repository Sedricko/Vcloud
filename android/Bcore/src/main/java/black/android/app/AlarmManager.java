package black.android.app;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.AlarmManager")
public interface AlarmManager {
    @BField
    IInterface mService();

    @BField
    int mTargetSdkVersion();
}
