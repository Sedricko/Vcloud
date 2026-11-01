package black.android.content.pm;

import android.content.pm.PackageParser.Package;

import java.io.File;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.content.pm.PackageParser")
public interface PackageParserLollipop {
    @BConstructor
    android.content.pm.PackageParser _new();
















    @BMethod
    void collectCertificates(Package p, int flags);

    @BMethod
    Package parsePackage(File File0, int flags);
}
