package black.android.app;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.LoadedApk")
public interface LoadedApkICS {
    @BField
    Object mCompatibilityInfo();
}
