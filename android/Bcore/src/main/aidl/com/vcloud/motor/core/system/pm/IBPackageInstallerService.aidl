// IBPackageInstallerService.aidl
package com.vcloud.motor.core.system.pm;

import com.vcloud.motor.core.system.pm.BPackageSettings;
import com.vcloud.motor.entity.pm.InstallOption;

// Declare any non-default types here with import statements

interface IBPackageInstallerService {
    int installPackageAsUser(in BPackageSettings ps, int userId);
    int uninstallPackageAsUser(in BPackageSettings ps, boolean removeApp, int userId);
    int clearPackage(in BPackageSettings ps, int userId);
    int updatePackage(in BPackageSettings ps);
}
