package black.android.rms.resource;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.rms.resource.ReceiverResource")
public interface ReceiverResourceM {
    @BField
    String[] mWhiteList();
}
