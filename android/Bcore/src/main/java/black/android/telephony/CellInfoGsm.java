package black.android.telephony;

import android.telephony.CellIdentityGsm;
import android.telephony.CellSignalStrengthGsm;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.telephony.CellInfoGsm")
public interface CellInfoGsm {
    @BConstructor
    CellInfoGsm _new();

    @BField
    CellIdentityGsm mCellIdentityGsm();

    @BField
    CellSignalStrengthGsm mCellSignalStrengthGsm();
}
