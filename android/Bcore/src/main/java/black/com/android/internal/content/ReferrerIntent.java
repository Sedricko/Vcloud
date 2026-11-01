package black.com.android.internal.content;

import android.content.Intent;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;

@BClassName("com.android.internal.content.ReferrerIntent")
public interface ReferrerIntent {
    @BConstructor
    Intent _new(Intent Intent0, String String1);
}
