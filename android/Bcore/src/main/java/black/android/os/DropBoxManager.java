package black.android.os;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.os.DropBoxManager")
public interface DropBoxManager {
    @BField
    IInterface mService();
}
