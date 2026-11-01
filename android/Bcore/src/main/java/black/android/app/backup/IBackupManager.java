package black.android.app.backup;

import android.os.IBinder;
import android.os.IInterface;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BStaticMethod;

@BClassName("android.app.backup.IBackupManager")
public interface IBackupManager {
    @BClassName("android.app.backup.IBackupManager$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
