package black.android.app;

import java.io.File;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;

@BClassName("android.app.SharedPreferencesImpl")
public interface SharedPreferencesImpl {
    @BConstructor
    SharedPreferencesImpl _new(File File0, int int1);
}
