package black.android.os;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.os.UserHandle")
public interface UserHandle {
    @BStaticMethod
    Integer myUserId();
}
