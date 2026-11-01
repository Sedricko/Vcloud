package black.com.android.internal.policy;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("com.android.internal.policy.PhoneWindow$WindowManagerHolder")
public interface PhoneWindow {
    @BStaticField
    IInterface sWindowManager();
}
