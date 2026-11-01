package black.android.content.pm;

import android.content.pm.PackageManager;
import android.os.IInterface;
import android.os.UserManager;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.content.pm.LauncherApps")
public interface LauncherApps {
    @BField
    PackageManager mPm();

    @BField
    IInterface mService();

    @BField
    UserManager mUserManager();
}
