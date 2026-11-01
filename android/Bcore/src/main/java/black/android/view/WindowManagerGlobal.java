package black.android.view;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.view.WindowManagerGlobal")
public interface WindowManagerGlobal {
    @BStaticField
    int ADD_PERMISSION_DENIED();

    @BStaticField
    IInterface sWindowManagerService();
}
