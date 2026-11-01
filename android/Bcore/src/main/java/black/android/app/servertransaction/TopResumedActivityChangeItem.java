package black.android.app.servertransaction;


import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.servertransaction.TopResumedActivityChangeItem")
public interface TopResumedActivityChangeItem {
    @BField
    Boolean mOnTop();
}
