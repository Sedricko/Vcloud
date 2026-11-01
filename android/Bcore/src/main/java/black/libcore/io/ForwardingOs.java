package black.libcore.io;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("libcore.io.ForwardingOs")
public interface ForwardingOs {
    @BField
    Object os();
}
