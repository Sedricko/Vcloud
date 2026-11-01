package black.android.content.pm;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.content.pm.ApplicationInfo")
public interface ApplicationInfoP {
    @BField
    String[] splitClassLoaderNames();

    @BMethod
    void setHiddenApiEnforcementPolicy(int int0);
}
