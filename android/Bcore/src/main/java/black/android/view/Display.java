package black.android.view;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.view.Display")
public interface Display {
    @BStaticField
    IInterface sWindowManager();
}
