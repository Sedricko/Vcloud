package black.android.content.pm;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.content.pm.ApplicationInfo")
public interface ApplicationInfoN {
    @BField
    String credentialEncryptedDataDir();

    @BField
    String credentialProtectedDataDir();

    @BField
    String deviceEncryptedDataDir();

    @BField
    String deviceProtectedDataDir();
}
