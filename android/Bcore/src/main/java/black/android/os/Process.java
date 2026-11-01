package black.android.os;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.os.Process")
public interface Process {
    @BStaticMethod
    void setArgV0(String String0);
}
