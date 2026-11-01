package black.android.renderscript;

import java.io.File;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.renderscript.RenderScriptCacheDir")
public interface RenderScriptCacheDir {
    @BStaticMethod
    void setupDiskCache(File File0);
}
