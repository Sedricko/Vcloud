package black.android.content.pm;

import android.content.pm.PackageParser.SigningDetails;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.content.pm.SigningInfo")
public interface SigningInfo {
    @BConstructor
    android.content.pm.SigningInfo _new(SigningDetails SigningDetails0);

    @BField
    SigningDetails mSigningDetails();
}
