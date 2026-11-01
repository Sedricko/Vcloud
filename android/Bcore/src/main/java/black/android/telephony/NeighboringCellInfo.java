package black.android.telephony;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.telephony.NeighboringCellInfo")
public interface NeighboringCellInfo {
    @BField
    int mCid();

    @BField
    int mLac();

    @BField
    int mRssi();
}
