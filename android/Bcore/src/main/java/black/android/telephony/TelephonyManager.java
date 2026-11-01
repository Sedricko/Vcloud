package black.android.telephony;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;
import com.vcloud.reflexao.annotation.BStaticMethod;


@BClassName("android.telephony.TelephonyManager")
public interface TelephonyManager {

    @BStaticMethod
    Object getSubscriberInfoService();

    @BStaticField
    boolean sServiceHandleCacheEnabled();

    @BStaticField
    IInterface sIPhoneSubInfo();
}
