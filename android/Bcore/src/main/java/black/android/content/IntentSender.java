package black.android.content;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.content.IntentSender")
public interface IntentSender {
    @BField
    IInterface mTarget();
}
