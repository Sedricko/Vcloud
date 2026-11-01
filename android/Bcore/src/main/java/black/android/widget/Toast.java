package black.android.widget;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.widget.Toast")
public interface Toast {
    @BStaticField
    IInterface sService();
}
