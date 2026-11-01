package black.android.webkit;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.webkit.IWebViewUpdateService")
public interface IWebViewUpdateService {
    @BMethod
    String getCurrentWebViewPackageName();

    @BMethod
    Object waitForAndGetProvider();
}
