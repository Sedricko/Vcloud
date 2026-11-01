package black.android.os;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticField;

@BClassName("android.os.Parcel")
public interface Parcel {
    @BStaticField
    int VAL_PARCELABLE();

    @BStaticField
    int VAL_PARCELABLEARRAY();
}
