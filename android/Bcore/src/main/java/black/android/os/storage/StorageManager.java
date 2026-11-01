package black.android.os.storage;

import android.os.storage.StorageVolume;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.os.storage.StorageManager")
public interface StorageManager {
    @BStaticMethod
    StorageVolume[] getVolumeList(int int0, int int1);
}
