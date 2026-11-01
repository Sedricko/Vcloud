package black.java.lang;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("java.lang.ThreadGroup")
public interface ThreadGroupN {
    @BField
    java.lang.ThreadGroup[] groups();

    @BField
    Integer ngroups();

    @BField
    java.lang.ThreadGroup parent();
}
