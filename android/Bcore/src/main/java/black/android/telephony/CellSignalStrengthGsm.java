package black.android.telephony;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.telephony.CellSignalStrengthGsm")
public interface CellSignalStrengthGsm {
    @BConstructor
    CellSignalStrengthGsm _new();

    @BField
    int mBitErrorRate();

    @BField
    int mSignalStrength();
}
