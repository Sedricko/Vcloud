package black.com.android.internal.view.inputmethod;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.view.inputmethod.InputMethodManager")
public interface InputMethodManager {
    @BField
    IInterface mService();
}
