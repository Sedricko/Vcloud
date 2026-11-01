package black.com.android.internal.net;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;


@BClassName("com.android.internal.net.VpnConfig")
public interface VpnConfig {
    @BField
    String user();

    @BField
    List<String> disallowedApplications();

    @BField
    List<String> allowedApplications();
}
