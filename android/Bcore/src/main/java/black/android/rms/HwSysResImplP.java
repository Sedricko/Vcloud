package black.android.rms;

import java.util.Map;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.rms.HwSysResImpl")
public interface HwSysResImplP {
    @BField
    Map<Integer, java.util.ArrayList<String>> mWhiteListMap();
}
