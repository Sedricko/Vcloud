package black.android.app;

import android.graphics.drawable.Icon;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.Notification")
public interface NotificationM {
    @BField
    Icon mLargeIcon();

    @BField
    Icon mSmallIcon();
}
