package black.android.security.net.config;

import android.content.Context;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.security.net.config.NetworkSecurityConfigProvider")
public interface NetworkSecurityConfigProvider {
    @BStaticMethod
    void install(Context Context0);
}
