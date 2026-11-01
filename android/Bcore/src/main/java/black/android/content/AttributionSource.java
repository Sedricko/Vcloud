package black.android.content;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BMethod;


@BClassName("android.content.AttributionSource")
public interface AttributionSource {
    @BField
    Object mAttributionSourceState();

    @BMethod
    Object getNext();
}
