package black.android.service.notification;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.service.notification.StatusBarNotification")
public interface StatusBarNotification {
    @BField
    Integer id();

    @BField
    String opPkg();

    @BField
    String pkg();

    @BField
    String tag();
}
