package black.android.webkit;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.webkit.WebViewFactory")
public interface WebViewFactory {
    @BStaticField
    Boolean sWebViewSupported();

    @BStaticMethod
    Object getUpdateService();
}
