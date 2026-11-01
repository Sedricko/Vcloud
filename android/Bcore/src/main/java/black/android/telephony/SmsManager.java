package black.android.telephony;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.telephony.SmsManager")
public interface SmsManager {
    @BMethod
    Boolean getAutoPersisting();

    @BMethod
    void setAutoPersisting(boolean boolean0);
}
