package black.android.app;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.Notification")
public interface NotificationO {
    @BField
    String mChannelId();

    @BField
    String mGroupKey();
}
