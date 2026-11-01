package black.android.app;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.NotificationChannelGroup")
public interface NotificationChannelGroup {
    @BField
    List<android.app.NotificationChannel> mChannels();

    @BField
    String mId();
}
