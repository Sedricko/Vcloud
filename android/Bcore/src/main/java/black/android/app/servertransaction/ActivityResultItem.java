package black.android.app.servertransaction;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.app.servertransaction.ActivityResultItem")
public interface ActivityResultItem {
    @BField
    List mResultInfoList();
}
