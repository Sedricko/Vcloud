package black.android.net.wifi;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.net.wifi.WifiSsid")
public interface WifiSsid {
    @BStaticMethod
    Object createFromAsciiEncoded(String asciiEncoded);
}
