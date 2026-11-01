package black.android.app;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.NotificationChannel")
public interface NotificationChannel {
    @BField
    String mId();
}
