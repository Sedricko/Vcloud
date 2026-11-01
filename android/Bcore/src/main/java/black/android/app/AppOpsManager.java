package black.android.app;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.AppOpsManager")
public interface AppOpsManager {
    @BField
    IInterface mService();
}
