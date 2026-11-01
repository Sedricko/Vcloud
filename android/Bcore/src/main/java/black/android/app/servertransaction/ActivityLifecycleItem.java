package black.android.app.servertransaction;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.app.servertransaction.ActivityLifecycleItem")
public interface ActivityLifecycleItem {
    @BMethod
    Integer getTargetState();
}
