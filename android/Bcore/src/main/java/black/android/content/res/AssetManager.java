package black.android.content.res;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.content.res.AssetManager")
public interface AssetManager {
    @BConstructor
    android.content.res.AssetManager _new();

    @BMethod
    Integer addAssetPath(String String0);

    @BMethod
    Configuration getConfiguration();

    @BMethod
    DisplayMetrics getDisplayMetrics();
}
