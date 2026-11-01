package black.android.app;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.app.WallpaperManager")
public interface WallpaperManager {
    @BStaticField
    Object sGlobals();

    @BClassName("android.app.WallpaperManager$Globals")
    interface Globals {
        @BField
        Object mService();
    }
}
