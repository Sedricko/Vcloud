package black.android.ddm;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.ddm.DdmHandleAppName")
public interface DdmHandleAppName {
    @BStaticMethod
    void setAppName(String String0, int i);
}
