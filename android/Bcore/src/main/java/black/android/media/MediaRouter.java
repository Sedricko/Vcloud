package black.android.media;

import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.media.MediaRouter")
public interface MediaRouter {
    @BStaticField
    Object sStatic();

    @BClassName("android.media.MediaRouter$Static")
    interface StaticKitkat {
        @BField
        IInterface mMediaRouterService();
    }

    @BClassName("android.media.MediaRouter$Static")
    interface Static {
        @BField
        IInterface mAudioService();
    }
}
