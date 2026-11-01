package black.java.io;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("java.io.File")
public interface File {
    @BStaticField
    Object fs();
}
