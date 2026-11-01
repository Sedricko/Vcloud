package black.libcore.io;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("libcore.io.Libcore")
public interface Libcore {
    @BStaticField
    Object os();
}
