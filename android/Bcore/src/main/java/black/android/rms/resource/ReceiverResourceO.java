package black.android.rms.resource;

import java.util.Map;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.rms.resource.ReceiverResource")
public interface ReceiverResourceO {
    @BField
    Map<Integer, java.util.List<String>> mWhiteListMap();
}
