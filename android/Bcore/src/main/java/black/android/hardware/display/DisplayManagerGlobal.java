package black.android.hardware.display;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.hardware.display.DisplayManagerGlobal")
public interface DisplayManagerGlobal {
    @BField
    IInterface mDm();

    @BStaticMethod
    Object getInstance();
}
