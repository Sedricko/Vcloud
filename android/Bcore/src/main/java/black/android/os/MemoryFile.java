package black.android.os;

import java.io.FileDescriptor;

import com.vcloud.reflexao.annotation.BClassName;
import com.vcloud.reflexao.annotation.BMethod;

@BClassName("android.os.MemoryFile")
public interface MemoryFile {
    @BMethod
    FileDescriptor getFileDescriptor();
}
