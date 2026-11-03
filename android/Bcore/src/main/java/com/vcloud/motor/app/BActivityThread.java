package com.vcloud.motor.app;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import java.io.File;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import black.android.app.ActivityThreadAppBindDataContext;
import black.android.app.BRActivity;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadActivityClientRecord;
import black.android.app.BRActivityThreadAppBindData;
import black.android.app.BRActivityThreadNMR1;
import black.android.app.BRActivityThreadQ;
import black.android.app.BRContextImpl;
import black.android.app.BRLoadedApk;
import black.android.app.BRService;
import black.android.app.LoadedApk;
import black.android.content.BRBroadcastReceiver;
import black.android.content.BRContentProviderClient;
import black.android.graphics.BRCompatibility;
import black.android.security.net.config.BRNetworkSecurityConfigProvider;
import black.com.android.internal.content.BRReferrerIntent;
import black.dalvik.system.BRVMRuntime;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.configuration.AppLifecycleCallback;
import com.vcloud.motor.app.dispatcher.AppServiceDispatcher;
import com.vcloud.motor.core.CrashHandler;
import com.vcloud.motor.core.IBActivityThread;
import com.vcloud.motor.core.IOCore;
import com.vcloud.motor.core.NativeCore;
import com.vcloud.motor.core.env.VirtualRuntime;
import com.vcloud.motor.core.system.user.BUserHandle;
import com.vcloud.motor.entity.AppConfig;
import com.vcloud.motor.entity.am.ReceiverData;

import com.vcloud.motor.fake.delegate.AppInstrumentation;
import com.vcloud.motor.fake.delegate.ContentProviderDelegate;

import com.vcloud.motor.fake.hook.HookManager;
import com.vcloud.motor.fake.service.HCallbackProxy;
import com.vcloud.motor.utils.Reflector;
import com.vcloud.motor.utils.SafeContextWrapper;
import com.vcloud.motor.utils.GlobalContextWrapper;
import com.vcloud.motor.utils.Slog;
import com.vcloud.motor.utils.compat.ActivityManagerCompat;
import com.vcloud.motor.utils.compat.BuildCompat;
import com.vcloud.motor.utils.compat.ContextCompat;
import com.vcloud.motor.utils.compat.StrictModeCompat;
import com.vcloud.motor.core.system.JarManager;

public class BActivityThread extends IBActivityThread.Stub {
    public static final String TAG = "BActivityThread";

    private static BActivityThread sBActivityThread;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private AppConfig mAppConfig;
    private final List<ProviderInfo> mProviders = new ArrayList<>();
    private final Handler mH = VcloudCore.get().getHandler();
    private static final Object mConfigLock = new Object();

    public static boolean isThreadInit() {
        return sBActivityThread != null;
    }

    public static BActivityThread currentActivityThread() {
        if (sBActivityThread == null) {
            synchronized (BActivityThread.class) {
                if (sBActivityThread == null) {
                    sBActivityThread = new BActivityThread();
                }
            }
        }
        return sBActivityThread;
    }

    public static AppConfig getAppConfig() {
        synchronized (mConfigLock) {
            return currentActivityThread().mAppConfig;
        }
    }

    public static List<ProviderInfo> getProviders() {
        return currentActivityThread().mProviders;
    }

    public static String getAppProcessName() {
        if (getAppConfig() != null) {
            return getAppConfig().processName;
        } else if (currentActivityThread().mBoundApplication != null) {
            return currentActivityThread().mBoundApplication.processName;
        } else {
            return null;
        }
    }

    public static String getAppPackageName() {
        if (getAppConfig() != null) {
            return getAppConfig().packageName;
        } else if (currentActivityThread().mInitialApplication != null) {
            return currentActivityThread().mInitialApplication.getPackageName();
        } else {
            return null;
        }
    }

    public static Application getApplication() {
        return currentActivityThread().mInitialApplication;
    }

    public static int getAppPid() {
        return getAppConfig() == null ? -1 : getAppConfig().bpid;
    }

    public static int getBUid() {
        return getAppConfig() == null ? BUserHandle.AID_APP_START : getAppConfig().buid;
    }

    public static int getBAppId() {
        return BUserHandle.getAppId(getBUid());
    }

    public static int getCallingBUid() {
        return getAppConfig() == null ? VcloudCore.getHostUid() : getAppConfig().callingBUid;
    }

    public static int getUid() {
        return getAppConfig() == null ? -1 : getAppConfig().uid;
    }

    public static int getUserId() {
        return getAppConfig() == null ? 0 : getAppConfig().userId;
    }

    public void initProcess(AppConfig appConfig) {
        synchronized (mConfigLock) {
            if (this.mAppConfig != null && !this.mAppConfig.packageName.equals(appConfig.packageName)) {
                
                throw new RuntimeException("reject init process: " + appConfig.processName + ", this process is : " + this.mAppConfig.processName);
            }
            this.mAppConfig = appConfig;
            IBinder iBinder = asBinder();
            try {
                iBinder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        synchronized (mConfigLock) {
                            try {
                                iBinder.linkToDeath(this, 0);
                            } catch (RemoteException ignored) {
                            }
                            mAppConfig = null;
                        }
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isInit() {
        return mBoundApplication != null;
    }

    public Service createService(ServiceInfo serviceInfo, IBinder token) {
        if (!BActivityThread.currentActivityThread().isInit()) {
            BActivityThread.currentActivityThread().bindApplication(serviceInfo.packageName, serviceInfo.processName);
        }
        ClassLoader classLoader = BRLoadedApk.get(mBoundApplication.info).getClassLoader();
        Service service;
        try {
            service = (Service) classLoader.loadClass(serviceInfo.name).newInstance();
        } catch (ClassNotFoundException e) {
            
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services class not found, skipping: " + serviceInfo.name);
                return null;
            }
            e.printStackTrace();
            Slog.e(TAG, "Unable to instantiate service " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Slog.e(TAG, "Unable to instantiate service " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        }

        try {
            Context context = VcloudCore.getContext().createPackageContext(
                    serviceInfo.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            BRContextImpl.get(context).setOuterContext(service);
            BRService.get(service).attach(
                    context,
                    VcloudCore.mainThread(),
                    serviceInfo.name,
                    token,
                    mInitialApplication,
                    BRActivityManagerNative.get().getDefault()
            );
            ContextCompat.fix(context);
            service.onCreate();
            return service;
        } catch (Exception e) {
            
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services service creation failed, skipping: " + serviceInfo.name);
                return null;
            }
            Slog.w(TAG, "Service creation failed, but continuing: " + serviceInfo.name + " - " + e.getMessage());
            return null;
        }
    }

    public JobService createJobService(ServiceInfo serviceInfo) {
        if (!BActivityThread.currentActivityThread().isInit()) {
            BActivityThread.currentActivityThread().bindApplication(serviceInfo.packageName, serviceInfo.processName);
        }
        ClassLoader classLoader = BRLoadedApk.get(mBoundApplication.info).getClassLoader();
        JobService service;
        try {
            service = (JobService) classLoader.loadClass(serviceInfo.name).newInstance();
        } catch (ClassNotFoundException e) {
            
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services JobService class not found, skipping: " + serviceInfo.name);
                return null;
            }
            e.printStackTrace();
            Slog.e(TAG, "Unable to create JobService " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Slog.e(TAG, "Unable to create JobService " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        }

        try {
            Context context = VcloudCore.getContext().createPackageContext(
                    serviceInfo.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            BRContextImpl.get(context).setOuterContext(service);
            BRService.get(service).attach(
                    context,
                    VcloudCore.mainThread(),
                    serviceInfo.name,
                    BActivityThread.currentActivityThread().getActivityThread(),
                    mInitialApplication,
                    BRActivityManagerNative.get().getDefault()
            );
            ContextCompat.fix(context);
            service.onCreate();
            service.onBind(null);
            return service;
        } catch (Exception e) {
            
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services JobService creation failed, skipping: " + serviceInfo.name);
                return null;
            }
            Slog.w(TAG, "JobService creation failed, but continuing: " + serviceInfo.name + " - " + e.getMessage());
            return null;
        }
    }

    public void bindApplication(final String packageName, final String processName) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            final ConditionVariable conditionVariable = new ConditionVariable();
            VcloudCore.get().getHandler().post(() -> {
                
                Object bindData = createBindApplicationData(packageName, processName);
                handleBindApplication(packageName, processName);
                conditionVariable.open();
            });
            conditionVariable.block();
        } else {
            
            Object bindData = createBindApplicationData(packageName, processName);
            handleBindApplication(packageName, processName);
        }
    }

    private Object createBindApplicationData(String packageName, String processName) {
        try {
            
            PackageInfo packageInfo = VcloudCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_PROVIDERS, getUserId());
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;

            return new Object() {
                public ApplicationInfo getInfo() { return applicationInfo; }
                public List<ProviderInfo> getProviders() { 
                    return packageInfo.providers != null ? Arrays.asList(packageInfo.providers) : new ArrayList<>();
                }
            };
        } catch (Exception e) {
            Slog.e(TAG, "Error creating bind application data", e);
            
            return new Object() {
                public ApplicationInfo getInfo() { return null; }
                public List<ProviderInfo> getProviders() { return new ArrayList<>(); }
            };
        }
    }

    public synchronized void handleBindApplication(String packageName, String processName) {
        if (isInit())
            return;
        try {
            CrashHandler.create();
        } catch (Throwable ignored) {
        }

        PackageInfo packageInfo = VcloudCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_PROVIDERS, BActivityThread.getUserId());
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (packageInfo.providers == null) {
            packageInfo.providers = new ProviderInfo[]{};
        }
        mProviders.addAll(Arrays.asList(packageInfo.providers));

        Object boundApplication = BRActivityThread.get(VcloudCore.mainThread()).mBoundApplication();

        Context packageContext = createPackageContext(applicationInfo);
        Object loadedApk = BRContextImpl.get(packageContext).mPackageInfo();
        BRLoadedApk.get(loadedApk)._set_mSecurityViolation(false);
        
        BRLoadedApk.get(loadedApk)._set_mApplicationInfo(applicationInfo);

        int targetSdkVersion = applicationInfo.targetSdkVersion;
        if (targetSdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy newPolicy = new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy()).permitNetwork().build();
            StrictMode.setThreadPolicy(newPolicy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (targetSdkVersion < Build.VERSION_CODES.N) {
                StrictModeCompat.disableDeathOnFileUriExposure();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(getUserId() + ":" + packageName + ":" + processName);
        }

        VirtualRuntime.setupRuntime(processName, applicationInfo);

        BRVMRuntime.get(BRVMRuntime.get().getRuntime()).setTargetSdkVersion(applicationInfo.targetSdkVersion);
        if (BuildCompat.isS()) {
            BRCompatibility.get().setTargetSdkVersion(applicationInfo.targetSdkVersion);
        }

        NativeCore.init(Build.VERSION.SDK_INT);
        assert packageContext != null;
        IOCore.get().enableRedirect(packageContext);

        AppBindData bindData = new AppBindData();
        bindData.appInfo = applicationInfo;
        bindData.processName = processName;
        bindData.info = loadedApk;
        bindData.providers = mProviders;

        ActivityThreadAppBindDataContext activityThreadAppBindData = BRActivityThreadAppBindData.get(boundApplication);
        activityThreadAppBindData._set_instrumentationName(new ComponentName(bindData.appInfo.packageName, Instrumentation.class.getName()));
        activityThreadAppBindData._set_appInfo(bindData.appInfo);
        activityThreadAppBindData._set_info(bindData.info);
        activityThreadAppBindData._set_processName(bindData.processName);
        activityThreadAppBindData._set_providers(bindData.providers);

        mBoundApplication = bindData;

        if (BRNetworkSecurityConfigProvider.getRealClass() != null) {
            Security.removeProvider("AndroidNSSP");
            BRNetworkSecurityConfigProvider.get().install(packageContext);
        }
        Application application;
        try {
            onBeforeCreateApplication(packageName, processName, packageContext);

            try {
                application = BRLoadedApk.get(loadedApk).makeApplication(false, null);
            } catch (Exception makeAppException) {
                Slog.e(TAG, "Failed to makeApplication, trying fallback approach", makeAppException);
                application = null;
            }

            if (application == null) {
                Slog.w(TAG, "makeApplication returned null, attempting fallback creation");

                try {
                    application = BRLoadedApk.get(loadedApk).makeApplication(true, null);
                } catch (Exception e) {
                    Slog.e(TAG, "Fallback makeApplication also failed", e);
                }

                if (application == null) {
                    Slog.w(TAG, "Creating minimal application context as fallback");
                    try {
                        
                        application = (Application) packageContext;
                        if (application == null) {
                            Slog.e(TAG, "Even package context is null, this is critical");
                            throw new RuntimeException("Unable to create application context");
                        }
                    } catch (Exception contextException) {
                        Slog.e(TAG, "Failed to create fallback application context", contextException);
                        throw new RuntimeException("Unable to makeApplication - all fallback attempts failed", contextException);
                    }
                }
            }
            
            if (application == null) {
                Slog.e(TAG, "makeApplication application Error! All attempts failed");
                throw new RuntimeException("Unable to create application - all creation methods failed");
            }
            
            mInitialApplication = application;
            BRActivityThread.get(VcloudCore.mainThread())._set_mInitialApplication(mInitialApplication);
            ContextCompat.fix((Context) BRActivityThread.get(VcloudCore.mainThread()).getSystemContext());
            ContextCompat.fix(mInitialApplication);
            installProviders(mInitialApplication, bindData.processName, bindData.providers);

            com.vcloud.motor.utils.Monitor.processo(processName);
            com.vcloud.motor.utils.Monitor.ev("CICLO-DE-VIDA", "processo do app iniciado: pacote=" + packageName + " processo=" + processName + " pid=" + android.os.Process.myPid()
                    + " uid(virtual)=" + android.os.Process.myUid());
            com.vcloud.motor.utils.Monitor.iniciar();
            com.vcloud.motor.utils.Monitor.saude();
            long antesOnCreate = System.currentTimeMillis();
            onBeforeApplicationOnCreate(packageName, processName, application);
            AppInstrumentation.get().callApplicationOnCreate(application);
            onAfterApplicationOnCreate(packageName, processName, application);
            com.vcloud.motor.utils.Monitor.ev("CICLO-DE-VIDA", packageName + " Application.onCreate terminou em " + (System.currentTimeMillis() - antesOnCreate) + " ms");

            // Vcloud: alguns jogos (ex.: DYSMANTLE) trocam o Instrumentation no Application.onCreate;
            // sem isto as telas deles não passam pelo AppInstrumentation (orientação deitada nunca era aplicada).
            Instrumentation atual = BRActivityThread.get(VcloudCore.mainThread()).mInstrumentation();
            if (!(atual instanceof AppInstrumentation)) {
                Slog.w(TAG, "O app trocou o Instrumentation por " + (atual == null ? "null" : atual.getClass().getName()) + "; recolocando o do Vcloud");
            }
            HookManager.get().checkEnv(AppInstrumentation.class);
            HookManager.get().checkEnv(HCallbackProxy.class);
            // Vcloud: janelas flutuantes do app não podem aparecer fora do Vcloud
            try { com.vcloud.motor.utils.GuardaSobreposicao.iniciar(); } catch (Throwable ignored) { /* sem a guarda o app abre igual */ }
            if (packageName.equals("com.android.vending") || packageName.equals("com.google.android.gms")) {
                com.vcloud.motor.utils.ProbeGoogle.medirDentro(mInitialApplication, packageName + " (" + processName + ")");
                if (packageName.equals("com.android.vending")) {
                    // diagnóstico (Play Store 53.8.11): política "internal-only" do grpc-binder guarda Process.myUid() de quando a classe carregou
                    try {
                        java.lang.reflect.Field f = Class.forName("cifm", true, application.getClassLoader()).getDeclaredField("a");
                        f.setAccessible(true);
                        com.vcloud.motor.utils.Diagnostico.evento("PLAYSTORE (" + processName + ") política internal-only: cifm.a=" + f.get(null)
                                + " myUid agora=" + android.os.Process.myUid() + " buid=" + getBUid() + " host=" + VcloudCore.getHostUid());
                    } catch (Throwable t) {
                        com.vcloud.motor.utils.Diagnostico.evento("PLAYSTORE (" + processName + ") não li cifm.a: " + t);
                    }
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Critical error in handleBindApplication", e);
            throw new RuntimeException("Unable to makeApplication", e);
        }
    }

    private ClassLoader getClassLoader(android.content.pm.ApplicationInfo appInfo) {
        try {
            
            String sourceDir = appInfo.sourceDir;
            if (sourceDir != null) {
                File codeFile = new File(sourceDir);
                if (codeFile.canWrite() && !codeFile.setReadOnly()) {
                    throw new SecurityException("Unable to make virtual APK read-only: " + sourceDir);
                }
                return new dalvik.system.PathClassLoader(sourceDir, ClassLoader.getSystemClassLoader());
            }

            return ClassLoader.getSystemClassLoader();
        } catch (Exception e) {
            Slog.w(TAG, "Error getting class loader: " + e.getMessage());
            return ClassLoader.getSystemClassLoader();
        }
    }

    private Application createApplication(android.content.pm.ApplicationInfo appInfo) {
        try {
            
            ClassLoader classLoader = getClassLoader(appInfo);
            Class<?> appClass = classLoader.loadClass(appInfo.className);
            Application application = (Application) appClass.newInstance();

            ensureApplicationBaseContext(application, appInfo);
            
            return application;
        } catch (Exception e) {
            Slog.e(TAG, "Error creating application: " + e.getMessage());
            return null;
        }
    }

    private void ensureApplicationBaseContext(Application application, android.content.pm.ApplicationInfo appInfo) {
        try {
            
            if (application.getBaseContext() != null) {
                Slog.d(TAG, "Application already has base context: " + appInfo.className);
                return;
            }

            Context packageContext = createPackageContext(appInfo);
            if (packageContext == null) {
                Slog.w(TAG, "Could not create package context for application: " + appInfo.className + ", using fallback");
                
                packageContext = createFallbackContext(appInfo.packageName);
            }

            if (packageContext == null) {
                Slog.e(TAG, "Failed to create any context for application: " + appInfo.className);
                return;
            }

            try {
                Method attachBaseContext = Application.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(application, packageContext);
                Slog.d(TAG, "Successfully attached base context to application: " + appInfo.className);
            } catch (Exception e) {
                Slog.w(TAG, "Could not attach base context to application: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error ensuring application base context: " + e.getMessage());
        }
    }

    private Context createFallbackContext(String packageName) {
        try {
            Context baseContext = VcloudCore.getContext();
            if (baseContext == null) {
                Slog.e(TAG, "VcloudCore.getContext() is null, cannot create fallback context");
                return null;
            }

            return new ContextWrapper(baseContext) {
                @Override
                public String getPackageName() {
                    return packageName;
                }
                
                @Override
                public android.content.pm.PackageManager getPackageManager() {
                    try {
                        return baseContext.getPackageManager();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting package manager from base context: " + e.getMessage());
                        return null;
                    }
                }
                
                @Override
                public android.content.res.Resources getResources() {
                    try {
                        return baseContext.getResources();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting resources from base context: " + e.getMessage());
                        try {
                            return android.content.res.Resources.getSystem();
                        } catch (Exception e2) {
                            Slog.e(TAG, "Error getting system resources: " + e2.getMessage());
                            return null;
                        }
                    }
                }
                
                @Override
                public ClassLoader getClassLoader() {
                    try {
                        return baseContext.getClassLoader();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting class loader from base context: " + e.getMessage());
                        try {
                            return ClassLoader.getSystemClassLoader();
                        } catch (Exception e2) {
                            Slog.e(TAG, "Error getting system class loader: " + e2.getMessage());
                            return null;
                        }
                    }
                }
                
                @Override
                public Context getApplicationContext() {
                    try {
                        return baseContext.getApplicationContext();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting application context from base context: " + e.getMessage());
                        return this;
                    }
                }
            };
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create fallback context for " + packageName + ": " + e.getMessage());
            return null;
        }
    }

    private List<android.content.pm.ProviderInfo> getProviderInfoList(Object data) {
        try {
            
            if (data != null) {
                try {
                    
                    Method getProvidersMethod = data.getClass().getMethod("getProviders");
                    List<ProviderInfo> providers = (List<ProviderInfo>) getProvidersMethod.invoke(data);
                    if (providers != null) {
                        return providers;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error getting providers from custom data object: " + e.getMessage());
                }
            }

            return new ArrayList<>();
        } catch (Exception e) {
            Slog.e(TAG, "Error getting provider info list", e);
            return new ArrayList<>();
        }
    }

    private void installContentProvider(Application application, android.content.pm.ProviderInfo providerInfo) {
        try {

            if (application == null) {
                Slog.w(TAG, "Application is null, cannot install content provider: " + providerInfo.name);
                return;
            }

            // Skip known anti-virtual-environment detection providers.
            // These providers detect the sandbox and call System.exit() to kill the app.
            // By skipping them, the app continues to run normally.
            if (providerInfo.name != null && isAntiDetectProvider(providerInfo.name)) {
                Slog.w(TAG, "Skipping anti-detect ContentProvider: " + providerInfo.name);
                return;
            }

            ClassLoader classLoader = application.getClassLoader();
            if (classLoader == null) {
                Slog.w(TAG, "Application class loader is null, using system class loader for: " + providerInfo.name);
                classLoader = ClassLoader.getSystemClassLoader();
            }

            android.content.ContentProvider provider = (android.content.ContentProvider) classLoader
                .loadClass(providerInfo.name).newInstance();

            provider.attachInfo(application, providerInfo);

            Slog.d(TAG, "Content provider installed: " + providerInfo.name);

        } catch (Exception e) {
            Slog.e(TAG, "Error installing content provider " + providerInfo.name, e);
        }
    }

    /**
     * Check if a ContentProvider is a known anti-virtual-environment detector.
     * These providers detect sandboxed environments and force-kill the app.
     */
    private static boolean isAntiDetectProvider(String className) {
        if (true) return false; // Vcloud: não pulamos verificações dos apps
        String lower = className.toLowerCase();
        // Meituan Hades anti-cheat
        return lower.contains("hades")
                || lower.contains("ztuni")
                // Douyin/TikTok security
                || lower.contains("securityguard")
                || lower.contains("avdetector")
                // Common detection patterns
                || lower.contains("virtualdetect")
                || lower.contains("sandboxdetect")
                || lower.contains("emulatordetect")
                || lower.contains("fridadetect")
                || lower.contains("hookdetect")
                || lower.contains("xposeddetect")
                || lower.contains("magiskdetect");
    }

    public static Context createPackageContext(ApplicationInfo info) {
        try {
            return VcloudCore.getContext().createPackageContext(info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Context createWrappedBaseContext(String packageName) {
        try {
            Context baseContext = VcloudCore.getContext();

            return new ContextWrapper(baseContext) {
                @Override
                public String getPackageName() {
                    return packageName;
                }
                
                @Override
                public PackageManager getPackageManager() {
                    return baseContext.getPackageManager();
                }
                
                @Override
                public Resources getResources() {
                    return baseContext.getResources();
                }
                
                @Override
                public ClassLoader getClassLoader() {
                    return baseContext.getClassLoader();
                }
                
                @Override
                public Context getApplicationContext() {
                    return baseContext.getApplicationContext();
                }
            };
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create wrapped base context for " + packageName + ": " + e.getMessage());
            
            return VcloudCore.getContext();
        }
    }

    private void installProviders(Context context, String processName, List<ProviderInfo> provider) {
        long origId = Binder.clearCallingIdentity();
        try {
            for (ProviderInfo providerInfo : provider) {
                try {
                    // Skip known anti-virtual-environment detection providers.
                    if (providerInfo.name != null && isAntiDetectProvider(providerInfo.name)) {
                        Slog.w(TAG, "Skipping anti-detect ContentProvider: " + providerInfo.name);
                        continue;
                    }
                    // Vcloud: os provedores do processo principal da Play Store (p2p, securityhub) não podem rodar no :background:
                    // lá o componente da loja é outro e a tarefa deles derrubava o processo ("Cannot cast spl to avru").
                    if (!processName.equals(context.getPackageName()) && "com.android.vending".equals(context.getPackageName())
                            && context.getPackageName().equals(providerInfo.processName)) {
                        continue;
                    }
                    if (processName.equals(providerInfo.processName) ||
                            providerInfo.processName.equals(context.getPackageName()) || providerInfo.multiprocess) {
                        installProvider(VcloudCore.mainThread(), context, providerInfo, null);
                        if (providerInfo.authority != null) {
                            for (String a : providerInfo.authority.split(";")) ContentProviderDelegate.LOCAIS.add(a);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
            ContentProviderDelegate.init();
        }
    }

    public Object getPackageInfo() {
        return mBoundApplication.info;
    }

    public static void installProvider(Object mainThread, Context context, ProviderInfo providerInfo, Object holder) throws Throwable {
        Method installProvider = Reflector.findMethodByFirstName(mainThread.getClass(), "installProvider");
        if (installProvider != null) {
            installProvider.setAccessible(true);
            installProvider.invoke(mainThread, context, holder, providerInfo, false, true, true);
        }
    }

    @Override
    public IBinder getActivityThread() {
        return BRActivityThread.get(VcloudCore.mainThread()).getApplicationThread();
    }

    @Override
    public void bindApplication() {
        if (!isInit()) {
            bindApplication(getAppPackageName(), getAppProcessName());
        }
    }

    @Override
    public void stopService(Intent intent) {
        AppServiceDispatcher.get().stopService(intent);
    }

    @Override
    public void restartJobService(String selfId) throws RemoteException {

    }

    @Override
    public IBinder acquireContentProviderClient(ProviderInfo providerInfo) throws RemoteException {
        if (!isInit()) {
            bindApplication(BActivityThread.getAppConfig().packageName, BActivityThread.getAppConfig().processName);
        }
        String[] split = providerInfo.authority.split(";");
        for (String auth : split) {
            ContentProviderClient contentProviderClient = VcloudCore.getContext()
                    .getContentResolver().acquireContentProviderClient(auth);
            IInterface iInterface = BRContentProviderClient.get(contentProviderClient).mContentProvider();
            if (iInterface == null)
                continue;
            return iInterface.asBinder();
        }
        return null;
    }

    @Override
    public void executarComando(final int comando) {
        mH.post(() -> {
            try {
                com.vcloud.motor.utils.ComandoDoPainel.executar(comando);
            } catch (Throwable e) {
                Slog.w(TAG, "Comando do painel falhou: " + e);
            }
        });
    }

    @Override
    public IBinder peekService(Intent intent) {
        return AppServiceDispatcher.get().peekService(intent);
    }

    @Override
    public void finishActivity(final IBinder token) {
        mH.post(() -> {
            Map<IBinder, Object> activities = BRActivityThread.get(VcloudCore.mainThread()).mActivities();
            if (activities.isEmpty())
                return;
            Object clientRecord = activities.get(token);
            if (clientRecord == null)
                return;
            Activity activity = getActivityByToken(token);

            while (activity.getParent() != null) {
                activity = activity.getParent();
            }

            int resultCode = BRActivity.get(activity).mResultCode();
            Intent resultData = BRActivity.get(activity).mResultData();
            ActivityManagerCompat.finishActivity(token, resultCode, resultData);
            BRActivity.get(activity)._set_mFinished(true);
        });
    }

    @Override
    public void handleNewIntent(final IBinder token, final Intent intent) {
        mH.post(() -> {
            Intent newIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                newIntent = BRReferrerIntent.get()._new(intent, VcloudCore.getHostPkg());
            } else {
                newIntent = intent;
            }
            Object mainThread = VcloudCore.mainThread();
            if (BRActivityThread.get(VcloudCore.mainThread())._check_performNewIntents(null, null) != null) {
                BRActivityThread.get(mainThread).performNewIntents(
                        token,
                        Collections.singletonList(newIntent)
                );
            } else if (BRActivityThreadNMR1.get(mainThread)._check_performNewIntents(null, null, false) != null) {
                BRActivityThreadNMR1.get(mainThread).performNewIntents(
                        token,
                        Collections.singletonList(newIntent),
                        true);
            } else if (BRActivityThreadQ.get(mainThread)._check_handleNewIntent(null, null) != null) {
                BRActivityThreadQ.get(mainThread).handleNewIntent(token, Collections.singletonList(newIntent));
            }
        });
    }

    @Override
    public void mudancaDePacote(Intent intent) throws RemoteException {
        com.vcloud.motor.fake.delegate.InnerReceiverDelegate.entregar(intent);
    }

    @Override
    public void scheduleReceiver(ReceiverData data) throws RemoteException {
        if (!isInit()) {
            bindApplication();
        }
        mH.post(() -> {
            BroadcastReceiver mReceiver = null;
            Intent intent = data.intent;
            ActivityInfo activityInfo = data.activityInfo;
            BroadcastReceiver.PendingResult pendingResult = data.data.build();

            try {
                Context baseContext = mInitialApplication.getBaseContext();
                ClassLoader classLoader = baseContext.getClassLoader();
                intent.setExtrasClassLoader(classLoader);

                mReceiver = (BroadcastReceiver) classLoader.loadClass(activityInfo.name).newInstance();
                BRBroadcastReceiver.get(mReceiver).setPendingResult(pendingResult);
                mReceiver.onReceive(baseContext, intent);
                BroadcastReceiver.PendingResult finish = BRBroadcastReceiver.get(mReceiver).getPendingResult();
                if (finish != null) {
                    finish.finish();
                }
                VcloudCore.getBActivityManager().finishBroadcast(data.data);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                Slog.e(TAG,
                        "Error receiving broadcast " + intent
                                + " in " + mReceiver);
            }
        });
    }

    public static Activity getActivityByToken(IBinder token) {
        Map<IBinder, Object> iBinderObjectMap =
                BRActivityThread.get(VcloudCore.mainThread()).mActivities();
        return BRActivityThreadActivityClientRecord.get(iBinderObjectMap.get(token)).activity();
    }

    private void onBeforeCreateApplication(String packageName, String processName, Context context) {
        for (AppLifecycleCallback appLifecycleCallback : VcloudCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeCreateApplication(packageName, processName, context, BActivityThread.getUserId());
        }
    }

    private void onBeforeApplicationOnCreate(String packageName, String processName, Application application) {
        for (AppLifecycleCallback appLifecycleCallback : VcloudCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeApplicationOnCreate(packageName, processName, application, BActivityThread.getUserId());
        }
    }

    private void onAfterApplicationOnCreate(String packageName, String processName, Application application) {
        for (AppLifecycleCallback appLifecycleCallback : VcloudCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.afterApplicationOnCreate(packageName, processName, application, BActivityThread.getUserId());
        }
    }

    public static void ensureActivityContext(Activity activity) {
        if (activity == null) {
            return;
        }
        
        try {
            
            Context currentContext = activity.getBaseContext();
            if (currentContext != null) {
                Slog.d(TAG, "Activity already has context: " + activity.getClass().getName());
                return;
            }
            
            Slog.w(TAG, "Activity has null context, ensuring valid context: " + activity.getClass().getName());

            Context validContext = null;
            try {
                validContext = getApplication();
                if (validContext == null) {
                    validContext = VcloudCore.getContext();
                }
            } catch (Exception e) {
                Slog.w(TAG, "Could not get application context: " + e.getMessage());
                validContext = VcloudCore.getContext();
            }
            
            if (validContext != null) {
                
                try {
                    Context packageContext = validContext.createPackageContext(
                        activity.getPackageName(),
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
                    );

                    java.lang.reflect.Method attachBaseContext = Activity.class.getDeclaredMethod("attachBaseContext", Context.class);
                    attachBaseContext.setAccessible(true);
                    attachBaseContext.invoke(activity, packageContext);
                    Slog.d(TAG, "Successfully attached package context to activity: " + activity.getClass().getName());
                } catch (Exception e) {
                    Slog.w(TAG, "Could not attach base context to activity: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error ensuring activity context: " + e.getMessage());
        }
    }

    public static void hookActivityThread() {
        try {
            
            Object activityThread = VcloudCore.mainThread();
            if (activityThread != null) {
                
                Instrumentation instrumentation = BRActivityThread.get(activityThread).mInstrumentation();
                if (instrumentation != null) {
                    Slog.d(TAG, "Found ActivityThread instrumentation, ensuring it's our AppInstrumentation");

                    if (!(instrumentation instanceof AppInstrumentation)) {
                        Slog.w(TAG, "ActivityThread instrumentation is not our AppInstrumentation, attempting to replace");

                        try {
                            AppInstrumentation appInstrumentation = AppInstrumentation.get();
                            appInstrumentation.injectHook();
                            Slog.d(TAG, "Successfully replaced ActivityThread instrumentation with AppInstrumentation");
                        } catch (Exception e) {
                            Slog.w(TAG, "Could not replace ActivityThread instrumentation: " + e.getMessage());
                        }
                    } else {
                        Slog.d(TAG, "ActivityThread instrumentation is already our AppInstrumentation");
                    }
                } else {
                    Slog.w(TAG, "ActivityThread instrumentation is null");
                }
            } else {
                Slog.w(TAG, "ActivityThread is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error hooking ActivityThread: " + e.getMessage());
        }
    }

    private Application createMinimalApplication(Context packageContext, String packageName) {
        try {
            Slog.d(TAG, "Creating minimal application for " + packageName);

            Application app = new Application() {
                @Override
                public void onCreate() {
                    super.onCreate();
                    Slog.d(TAG, "Minimal application onCreate called for " + packageName);
                }
                
                @Override
                public String getPackageName() {
                    return packageName;
                }
                
                @Override
                public Context getApplicationContext() {
                    return this;
                }
            };

            if (packageContext != null) {
                try {
                    Method attachBaseContext = Application.class.getDeclaredMethod("attachBaseContext", Context.class);
                    attachBaseContext.setAccessible(true);
                    attachBaseContext.invoke(app, packageContext);
                    Slog.d(TAG, "Successfully attached base context to minimal application for " + packageName);
                } catch (Exception e) {
                    Slog.w(TAG, "Could not attach base context to minimal application: " + e.getMessage());
                }
            } else {
                Slog.w(TAG, "Package context is null, cannot attach base context to minimal application");
            }
            
            Slog.d(TAG, "Minimal application created successfully for " + packageName);
            return app;
        } catch (Exception e) {
            Slog.e(TAG, "Error creating minimal application for " + packageName, e);
            return null;
        }
    }

    public static class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }
}
