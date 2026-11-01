package black.java.lang;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("java.lang.ThreadGroup")
public interface ThreadGroup {
    @BField
    List<java.lang.ThreadGroup> groups();

    @BField
    java.lang.ThreadGroup parent();
}
