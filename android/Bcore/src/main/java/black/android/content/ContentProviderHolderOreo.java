package black.android.content;

import android.content.pm.ProviderInfo;
import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.ContentProviderHolder")
public interface ContentProviderHolderOreo {
    @BField
    ProviderInfo info();

    @BField
    boolean noReleaseNeeded();

    @BField
    IInterface provider();
}
