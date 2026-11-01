package black.android.content;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;


@BClassName("android.content.AttributionSourceState")
public interface AttributionSourceState {
    @BField
    String packageName();

    @BField
    int uid();
}
