package black.android.content;

import android.content.Intent;
import android.os.Bundle;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.content.IIntentReceiver")
public interface IIntentReceiver {
    @BMethod
    void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser);
}
