package black.android.app.job;

import android.os.IBinder;
import android.os.PersistableBundle;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.job.JobParameters")
public interface JobParameters {
    @BField
    IBinder callback();

    @BField
    PersistableBundle extras();

    @BField
    int jobId();
}
