package black.android.os;

import android.os.Parcel;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.os.Bundle")
public interface BundleICS {
    @BField
    Parcel mParcelledData();
}
