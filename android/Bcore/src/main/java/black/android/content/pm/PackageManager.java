package black.android.content.pm;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BMethod;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.content.pm.PackageManager")
public interface PackageManager {
    @BStaticMethod
    void disableApplicationInfoCache();
}
