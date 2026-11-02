package com.vcloud.motor.fake.hook;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.fake.delegate.AppInstrumentation;

import com.vcloud.motor.fake.service.HCallbackProxy;
import com.vcloud.motor.fake.service.IAccessibilityManagerProxy;
import com.vcloud.motor.fake.service.IAccountManagerProxy;
import com.vcloud.motor.fake.service.IActivityClientProxy;
import com.vcloud.motor.fake.service.IActivityManagerProxy;
import com.vcloud.motor.fake.service.IActivityTaskManagerProxy;
import com.vcloud.motor.fake.service.IAlarmManagerProxy;
import com.vcloud.motor.fake.service.IAppOpsManagerProxy;
import com.vcloud.motor.fake.service.IAppWidgetManagerProxy;
import com.vcloud.motor.fake.service.IAttributionSourceProxy;
import com.vcloud.motor.fake.service.IAutofillManagerProxy;
import com.vcloud.motor.fake.service.ISensitiveContentProtectionManagerProxy;
import com.vcloud.motor.fake.service.ISettingsSystemProxy;
import com.vcloud.motor.fake.service.IConnectivityManagerProxy;
import com.vcloud.motor.fake.service.ISystemSensorManagerProxy;
import com.vcloud.motor.fake.service.IContentProviderProxy;
import com.vcloud.motor.fake.service.IXiaomiAttributionSourceProxy;
import com.vcloud.motor.fake.service.IXiaomiSettingsProxy;
import com.vcloud.motor.fake.service.IXiaomiMiuiServicesProxy;
import com.vcloud.motor.fake.service.IDnsResolverProxy;
import com.vcloud.motor.fake.service.IContextHubServiceProxy;
import com.vcloud.motor.fake.service.IDeviceIdentifiersPolicyProxy;
import com.vcloud.motor.fake.service.IDevicePolicyManagerProxy;
import com.vcloud.motor.fake.service.IDisplayManagerProxy;
import com.vcloud.motor.fake.service.IFingerprintManagerProxy;
import com.vcloud.motor.fake.service.IGraphicsStatsProxy;
import com.vcloud.motor.fake.service.IJobServiceProxy;
import com.vcloud.motor.fake.service.ILauncherAppsProxy;
import com.vcloud.motor.fake.service.ILocationManagerProxy;
import com.vcloud.motor.fake.service.IMediaRouterServiceProxy;
import com.vcloud.motor.fake.service.IMediaSessionManagerProxy;
import com.vcloud.motor.fake.service.IAudioServiceProxy;
import com.vcloud.motor.fake.service.ISensorPrivacyManagerProxy;
import com.vcloud.motor.fake.service.ContentResolverProxy;
import com.vcloud.motor.fake.service.IMiuiSecurityManagerProxy;
import com.vcloud.motor.fake.service.SystemLibraryProxy;
import com.vcloud.motor.fake.service.ReLinkerProxy;
import com.vcloud.motor.fake.service.MediaRecorderProxy;
import com.vcloud.motor.fake.service.NetworkPermissionCompat;
import com.vcloud.motor.fake.service.AudioRecordProxy;
import com.vcloud.motor.fake.service.MediaRecorderClassProxy;
import com.vcloud.motor.fake.service.SQLiteDatabaseProxy;
import com.vcloud.motor.fake.service.ClassLoaderProxy;
import com.vcloud.motor.fake.service.FileSystemProxy;
import com.vcloud.motor.fake.service.GmsProxy;
import com.vcloud.motor.fake.service.LevelDbProxy;
import com.vcloud.motor.fake.service.DeviceIdProxy;
import com.vcloud.motor.fake.service.GoogleAccountManagerProxy;
import com.vcloud.motor.fake.service.AuthenticationProxy;
import com.vcloud.motor.fake.service.AndroidIdProxy;
import com.vcloud.motor.fake.service.AudioPermissionProxy;
import com.vcloud.motor.fake.service.NetworkPermissionCompat;

import com.vcloud.motor.fake.service.INetworkManagementServiceProxy;
import com.vcloud.motor.fake.service.INotificationManagerProxy;
import com.vcloud.motor.fake.service.IPackageManagerProxy;
import com.vcloud.motor.fake.service.IPermissionManagerProxy;
import com.vcloud.motor.fake.service.IPersistentDataBlockServiceProxy;
import com.vcloud.motor.fake.service.IPhoneSubInfoProxy;
import com.vcloud.motor.fake.service.IPowerManagerProxy;
import com.vcloud.motor.fake.service.ApkAssetsProxy;
import com.vcloud.motor.fake.service.ResourcesManagerProxy;
import com.vcloud.motor.fake.service.IShortcutManagerProxy;
import com.vcloud.motor.fake.service.IStorageManagerProxy;
import com.vcloud.motor.fake.service.IStorageStatsManagerProxy;
import com.vcloud.motor.fake.service.ISystemUpdateProxy;
import com.vcloud.motor.fake.service.ITelephonyManagerProxy;
import com.vcloud.motor.fake.service.ITelephonyRegistryProxy;
import com.vcloud.motor.fake.service.IUserManagerProxy;
import com.vcloud.motor.fake.service.IVibratorServiceProxy;
import com.vcloud.motor.fake.service.IVpnManagerProxy;
import com.vcloud.motor.fake.service.IWifiManagerProxy;
import com.vcloud.motor.fake.service.IWifiScannerProxy;
import com.vcloud.motor.fake.service.IWindowManagerProxy;
import com.vcloud.motor.fake.service.context.ContentServiceStub;
import com.vcloud.motor.fake.service.context.RestrictionsManagerStub;
import com.vcloud.motor.fake.service.libcore.OsStub;
import com.vcloud.motor.utils.Slog;
import com.vcloud.motor.utils.compat.BuildCompat;
import com.vcloud.motor.fake.service.ISettingsProviderProxy;
import com.vcloud.motor.fake.service.FeatureFlagUtilsProxy;
import com.vcloud.motor.fake.service.WorkManagerProxy;



public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    public void init() {
        if (VcloudCore.get().isBlackProcess() || VcloudCore.get().isServerProcess()) {
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new ISensorPrivacyManagerProxy());
            addInjector(new ContentResolverProxy());
            addInjector(new SystemLibraryProxy());
            addInjector(new ReLinkerProxy());
            addInjector(new WorkManagerProxy());
            addInjector(new MediaRecorderProxy());
            addInjector(new AudioRecordProxy());
            addInjector(new IMiuiSecurityManagerProxy());
            addInjector(new ISettingsProviderProxy());
            addInjector(new FeatureFlagUtilsProxy());
            addInjector(new MediaRecorderClassProxy());
            addInjector(new SQLiteDatabaseProxy());
            addInjector(new ClassLoaderProxy());
            addInjector(new FileSystemProxy());
            addInjector(new GmsProxy());
            addInjector(new LevelDbProxy());
            addInjector(new DeviceIdProxy());
            addInjector(new GoogleAccountManagerProxy());
            addInjector(new AuthenticationProxy());
            addInjector(new AndroidIdProxy());
            addInjector(new AudioPermissionProxy());
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new IAccountManagerProxy());
            addInjector(new NetworkPermissionCompat());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IDnsResolverProxy());
                    addInjector(new IAttributionSourceProxy());
        addInjector(new IContentProviderProxy());
        addInjector(new ISettingsSystemProxy());
        addInjector(new ISystemSensorManagerProxy());
        
        
        addInjector(new IXiaomiAttributionSourceProxy());
        addInjector(new IXiaomiSettingsProxy());
        addInjector(new IXiaomiMiuiServicesProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            addInjector(AppInstrumentation.get());
            
            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new ApkAssetsProxy());
            addInjector(new ResourcesManagerProxy());
            
            if (BuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
            }
            
            if (BuildCompat.isS()) {
                addInjector(new ISensitiveContentProtectionManagerProxy());
            }
            
            if (BuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
                addInjector(new com.vcloud.motor.fake.service.IAuthServiceProxy());
                addInjector(new com.vcloud.motor.fake.service.IBiometricServiceProxy());
                addInjector(new com.vcloud.motor.fake.service.ISubProxy());
                addInjector(new com.vcloud.motor.fake.service.IBackupManagerProxy());
                addInjector(new com.vcloud.motor.fake.service.IAppSearchProxy());
                addInjector(new com.vcloud.motor.fake.service.ICrossProfileAppsProxy());
                addInjector(new com.vcloud.motor.fake.service.IUsageStatsProxy());
            }
            
            if (BuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }
            
            if (BuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }
            
            if (BuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }
            
            if (BuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }
            
            if (BuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }
            
            if (BuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }

            if (android.os.Build.VERSION.SDK_INT >= 33) {
                addInjector(new com.vcloud.motor.fake.service.ILocaleManagerProxy());
            }
            
            if (BuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook iInjectHook = mInjectors.get(clazz);
        if (iInjectHook != null && iInjectHook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
            iInjectHook.injectHook();
        }
    }

    public void checkAll() {
        for (Class<?> aClass : mInjectors.keySet()) {
            IInjectHook iInjectHook = mInjectors.get(aClass);
            if (iInjectHook != null && iInjectHook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
                iInjectHook.injectHook();
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass(), injectHook);
    }

    void injectAll() {
        for (IInjectHook value : mInjectors.values()) {
            try {
                Slog.d(TAG, "hook: " + value);
                value.injectHook();
            } catch (Exception e) {
                Slog.d(TAG, "hook error: " + value);
                
                handleHookError(value, e);
            }
        }
    }

    
    private void handleHookError(IInjectHook hook, Exception e) {
        String hookName = hook.getClass().getSimpleName();
        
        
        Slog.e(TAG, "Hook failed: " + hookName + " - " + e.getMessage(), e);
        
        
        if (hookName.contains("ActivityManager") || 
            hookName.contains("PackageManager") ||
            hookName.contains("WebView") ||
            hookName.contains("ContentProvider")) {
            
            Slog.w(TAG, "Critical hook failed: " + hookName + ", attempting recovery");
            
            try {
                
                if (hook.isBadEnv()) {
                    Slog.d(TAG, "Attempting to recover hook: " + hookName);
                    hook.injectHook();
                }
            } catch (Exception recoveryException) {
                Slog.e(TAG, "Hook recovery failed: " + hookName, recoveryException);
            }
        }
    }

    
    public boolean areCriticalHooksInstalled() {
        String[] criticalHooks = {
            "IActivityManagerProxy",
            "IPackageManagerProxy",
            "IContentProviderProxy"
        };
        
        for (String hookName : criticalHooks) {
            boolean found = false;
            for (Class<?> hookClass : mInjectors.keySet()) {
                if (hookClass.getSimpleName().equals(hookName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Slog.w(TAG, "Critical hook missing: " + hookName);
                return false;
            }
        }
        
        Slog.d(TAG, "All critical hooks are installed");
        return true;
    }

    
    public void reinitializeHooks() {
        Slog.d(TAG, "Reinitializing all hooks");
        
        
        mInjectors.clear();
        
        
        init();
        
        Slog.d(TAG, "Hook reinitialization completed");
    }
}
