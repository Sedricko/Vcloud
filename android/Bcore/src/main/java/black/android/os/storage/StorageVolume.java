package black.android.os.storage;

import java.io.File;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.os.storage.StorageVolume")
public interface StorageVolume {
    @BField
    File mInternalPath();

    @BField
    File mPath();
}
