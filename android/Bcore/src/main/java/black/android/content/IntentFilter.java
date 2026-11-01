package black.android.content;

import java.util.List;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BField;

@BClassName("android.content.IntentFilter")
public interface IntentFilter {
    @BField
    List<String> mActions();

    @BField
    List<String> mCategories();
}
