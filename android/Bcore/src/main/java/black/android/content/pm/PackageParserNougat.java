package black.android.content.pm;


import android.content.pm.PackageParser;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.content.pm.PackageParser")
public interface PackageParserNougat {
    @BStaticMethod
    void collectCertificates(PackageParser.Package p, int flags);
}
