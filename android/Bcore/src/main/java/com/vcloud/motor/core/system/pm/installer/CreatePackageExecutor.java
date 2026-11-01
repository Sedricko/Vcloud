package com.vcloud.motor.core.system.pm.installer;

import com.vcloud.motor.core.env.BEnvironment;
import com.vcloud.motor.core.system.pm.BPackageSettings;
import com.vcloud.motor.entity.pm.InstallOption;
import com.vcloud.motor.utils.FileUtils;


public class CreatePackageExecutor implements Executor {

    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));

        
        FileUtils.mkdirs(BEnvironment.getAppDir(ps.pkg.packageName));
        FileUtils.mkdirs(BEnvironment.getAppLibDir(ps.pkg.packageName));
        return 0;
    }
}
