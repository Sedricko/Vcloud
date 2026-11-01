package black.android.net.wifi;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.net.wifi.WifiScanner")
public interface WifiScanner {
    @BStaticField
    String GET_AVAILABLE_CHANNELS_EXTRA();
}
