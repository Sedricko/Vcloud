package black.android.content;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.content.ClipboardManager")
public interface ClipboardManagerOreo {
    @BStaticField
    IInterface sService();

    @BField
    IInterface mService();
}
