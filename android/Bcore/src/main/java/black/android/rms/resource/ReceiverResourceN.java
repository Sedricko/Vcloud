package black.android.rms.resource;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.rms.resource.ReceiverResource")
public interface ReceiverResourceN {
    @BField
    List<String> mWhiteList();
}
