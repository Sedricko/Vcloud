package black.android.app.job;

import android.content.Intent;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BConstructor;
import com.vcloud.reflexao.annotation.BField;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.app.job.JobWorkItem")
public interface JobWorkItem {
    @BConstructor
    JobWorkItem _new(Intent Intent0);

    @BField
    int mDeliveryCount();

    @BField
    Object mGrants();

    @BField
    int mWorkId();

    @BMethod
    Intent getIntent();
}
