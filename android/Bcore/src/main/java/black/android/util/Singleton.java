package black.android.util;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.util.Singleton")
public interface Singleton {
    @BField
    Object mInstance();

    @BMethod
    Object get();
}
