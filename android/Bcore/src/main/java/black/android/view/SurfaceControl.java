package black.android.view;

import android.graphics.Bitmap;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.view.SurfaceControl")
public interface SurfaceControl {
    @BStaticMethod
    Bitmap screnshot(int int0, int int1);
}
