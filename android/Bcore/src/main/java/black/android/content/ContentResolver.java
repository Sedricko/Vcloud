package black.android.content;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.content.ContentResolver")
public interface ContentResolver {
    @BStaticField
    IInterface sContentService();

    @BField
    String mPackageName();
}
