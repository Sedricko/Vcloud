package com.vcloud.motor.core.system.os;

import android.net.Uri;
import android.os.Process;
import android.os.RemoteException;
import android.os.storage.StorageVolume;

import java.io.File;

import black.android.os.storage.BRStorageManager;
import black.android.os.storage.BRStorageVolume;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.core.env.BEnvironment;
import com.vcloud.motor.core.system.ISystemService;
import com.vcloud.motor.core.system.user.BUserHandle;
import com.vcloud.motor.fake.provider.FileProvider;
import com.vcloud.motor.proxy.ProxyManifest;
import com.vcloud.motor.utils.compat.BuildCompat;


public class BStorageManagerService extends IBStorageManagerService.Stub implements ISystemService {
    private static final BStorageManagerService sService = new BStorageManagerService();

    public static BStorageManagerService get() {
        return sService;
    }

    public BStorageManagerService() {
    }

    @Override
    public StorageVolume[] getVolumeList(int uid, String packageName, int flags, int userId) throws RemoteException {
        if (BRStorageManager.get().getVolumeList(0, 0) == null) {
            return null;
        }
        try {
            StorageVolume[] storageVolumes = BRStorageManager.get().getVolumeList(BUserHandle.getUserId(Process.myUid()), 0);
            if (storageVolumes == null)
                return null;
            for (StorageVolume storageVolume : storageVolumes) {
                BRStorageVolume.get(storageVolume)._set_mPath(BEnvironment.getExternalUserDir(userId));
                if (BuildCompat.isPie()) {
                    BRStorageVolume.get(storageVolume)._set_mInternalPath(BEnvironment.getExternalUserDir(userId));
                }
            }
            return storageVolumes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Uri getUriForFile(String file) throws RemoteException {
        return FileProvider.getUriForFile(VcloudCore.getContext(), ProxyManifest.getProxyFileProvider(), new File(file));
    }

    @Override
    public void systemReady() {

    }
}
