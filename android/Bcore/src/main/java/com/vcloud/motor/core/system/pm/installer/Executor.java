package com.vcloud.motor.core.system.pm.installer;

import com.vcloud.motor.core.system.pm.BPackageSettings;
import com.vcloud.motor.entity.pm.InstallOption;


public interface Executor {
    public static final String TAG = "InstallExecutor";

    int exec(BPackageSettings ps, InstallOption option, int userId);
}
