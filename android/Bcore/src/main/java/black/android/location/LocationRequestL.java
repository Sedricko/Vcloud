package black.android.location;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.location.LocationRequest")
public interface LocationRequestL {
    @BField
    boolean mHideFromAppOps();

    @BField
    String mProvider();

    @BField
    Object mWorkSource();

    @BMethod
    String getProvider();
}
