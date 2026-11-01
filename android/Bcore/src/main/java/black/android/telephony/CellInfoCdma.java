package black.android.telephony;

import android.telephony.CellIdentityCdma;
import android.telephony.CellSignalStrengthCdma;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.telephony.CellInfoCdma")
public interface CellInfoCdma {
    @BConstructor
    CellInfoCdma _new();

    @BField
    CellIdentityCdma mCellIdentityCdma();

    @BField
    CellSignalStrengthCdma mCellSignalStrengthCdma();
}
