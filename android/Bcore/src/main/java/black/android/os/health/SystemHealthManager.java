package black.android.os.health;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.os.health.SystemHealthManager")
public interface SystemHealthManager {
    @BField
    IInterface mBatteryStats();
}
