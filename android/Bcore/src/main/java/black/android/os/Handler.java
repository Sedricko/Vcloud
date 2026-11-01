package black.android.os;

import android.os.Handler.Callback;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.os.Handler")
public interface Handler {
    @BField
    Callback mCallback();
}
