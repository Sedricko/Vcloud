package black.android.app;

import java.io.File;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.ContextImpl")
public interface ContextImplICS {
    @BField
    File mExternalCacheDir();

    @BField
    File mExternalFilesDir();
}
