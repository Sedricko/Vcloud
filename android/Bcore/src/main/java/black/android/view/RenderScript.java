package black.android.view;

import java.io.File;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.renderscript.RenderScript")
public interface RenderScript {
    @BStaticMethod
    void setupDiskCache(File File0);
}
