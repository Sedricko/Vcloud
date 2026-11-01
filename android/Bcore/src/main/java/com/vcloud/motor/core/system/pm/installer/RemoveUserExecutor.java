package com.vcloud.motor.core.system.pm.installer;

import com.vcloud.motor.core.env.BEnvironment;
import com.vcloud.motor.core.system.pm.BPackageSettings;
import com.vcloud.motor.entity.pm.InstallOption;
import com.vcloud.motor.utils.FileUtils;


public class RemoveUserExecutor implements Executor {

    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        String packageName = ps.pkg.packageName;
        
        FileUtils.deleteDir(BEnvironment.getDataDir(packageName, userId));
        FileUtils.deleteDir(BEnvironment.getDeDataDir(packageName, userId));
        FileUtils.deleteDir(BEnvironment.getExternalDataDir(packageName, userId));
        return 0;
    }
}
