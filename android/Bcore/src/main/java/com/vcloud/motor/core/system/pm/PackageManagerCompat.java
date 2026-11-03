package com.vcloud.motor.core.system.pm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import java.util.HashSet;
import java.util.Set;

import black.android.content.pm.BRApplicationInfoL;
import black.android.content.pm.BRApplicationInfoN;
import black.android.content.pm.BRPackageParserSigningDetails;
import black.android.content.pm.BRSigningInfo;
import black.android.content.res.BRAssetManager;
import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.core.env.AppSystemEnv;
import com.vcloud.motor.core.env.BEnvironment;
import com.vcloud.motor.entity.pm.InstallOption;
import com.vcloud.motor.utils.ArrayUtils;
import com.vcloud.motor.utils.FileUtils;
import com.vcloud.motor.utils.compat.BuildCompat;


@SuppressLint({"SdCardPath", "NewApi"})
public class PackageManagerCompat {

    /**
     * Vcloud: a Play Store pergunta com MATCH_ANY_USER (0x00400000). Repassar isso à consulta REAL do celular dá SecurityException
     * (o Vcloud não tem INTERACT_ACROSS_USERS), o PackageInfo todo saía nulo e a loja dizia "Package ... not installed"
     * (relatório 10:11, 4.0.72+: "PACOTE getPackageInfo falhou ... MATCH_ANY_USER flag requires INTERACT_ACROSS_USERS").
     */
    static int semParaTodosUsuarios(int flags) {
        return flags & ~0x00400000;
    }

    public static PackageInfo generatePackageInfo(BPackageSettings ps, int flags, BPackageUserState state, int userId) {
        if (ps == null) {
            return null;
        }
        BPackage p = ps.pkg;
        if (p != null) {
            PackageInfo packageInfo = null;
            try {
                packageInfo = generatePackageInfo(p, flags, 0, 0, state, userId);
            } catch (Throwable t) {
                // Vcloud: antes o erro sumia e o app parecia "não instalado" (a Play Store: "PSR: Package ... not installed")
                com.vcloud.motor.utils.Diagnostico.eventoLimitado("pkginfo|" + p.packageName, 30_000,
                        "PACOTE getPackageInfo falhou para " + p.packageName + " (flags=" + flags + "): " + t);
            }
            return packageInfo;
        }
        return null;
    }

    public static PackageInfo generatePackageInfo(BPackage p, int flags, long firstInstallTime, long lastUpdateTime, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo)) {
            return null;
        }

        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.versionCode = p.mVersionCode;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.sharedUserLabel = p.mSharedUserLabel;
        pi.applicationInfo = generateApplicationInfo(p, flags, state, userId);

        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if (!p.requestedPermissions.isEmpty()) {
            String[] requestedPermissions = new String[p.requestedPermissions.size()];
            p.requestedPermissions.toArray(requestedPermissions);
            pi.requestedPermissions = requestedPermissions;
        }

        if ((flags & PackageManager.GET_GIDS) != 0) {
            pi.gids = new int[]{};
        }
        if ((flags & PackageManager.GET_CONFIGURATIONS) != 0) {
            int N = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N > 0) {
                pi.configPreferences = new ConfigurationInfo[N];
                p.configPreferences.toArray(pi.configPreferences);
            }
            N = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N > 0) {
                pi.reqFeatures = new FeatureInfo[N];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
        }
        if ((flags & PackageManager.GET_ACTIVITIES) != 0) {
            pi.activities = null;
            final int N = p.activities.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Activity a = p.activities.get(i);
                    res[num++] = generateActivityInfo(a, flags, state, userId);
                }
                pi.activities = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_RECEIVERS) != 0) {
            pi.receivers = null;
            final int N = p.receivers.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Activity a = p.receivers.get(i);
                    res[num++] = generateActivityInfo(a, flags, state, userId);
                }
                pi.receivers = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_SERVICES) != 0) {
            pi.services = null;
            final int N = p.services.size();
            if (N > 0) {
                int num = 0;
                final ServiceInfo[] res = new ServiceInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Service s = p.services.get(i);
                    
                    ServiceInfo serviceInfo = generateServiceInfo(s, flags, state, userId);
                    if(serviceInfo == null) continue;
                    if ((flags & PackageManager.GET_META_DATA) != 0) {
                        if(serviceInfo.metaData != null){
                            res[num++] = serviceInfo;
                        }
                    }else{
                        res[num++] = serviceInfo;
                    }
                }
                pi.services = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_PROVIDERS) != 0) {
            pi.providers = null;
            final int N = p.providers.size();
            if (N > 0) {
                int num = 0;
                final ProviderInfo[] res = new ProviderInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Provider pr = p.providers.get(i);
                    ProviderInfo providerInfo = generateProviderInfo(pr, flags, state, userId);
                    if (providerInfo != null) {
                        res[num++] = providerInfo;
                    }
                }
                pi.providers = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_INSTRUMENTATION) != 0) {
            pi.instrumentation = null;
            int N = p.instrumentation.size();
            if (N > 0) {
                pi.instrumentation = new InstrumentationInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.instrumentation[i] = generateInstrumentationInfo(
                            p.instrumentation.get(i), flags);
                }
            }
        }
        if ((flags & PackageManager.GET_PERMISSIONS) != 0) {
            pi.permissions = null;
            int N = p.permissions.size();
            if (N > 0) {
                pi.permissions = new PermissionInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.permissions[i] = generatePermissionInfo(p.permissions.get(i), flags);
                }
            }
            pi.requestedPermissions = null;
            N = p.requestedPermissions.size();
            if (N > 0) {
                pi.requestedPermissions = new String[N];
                pi.requestedPermissionsFlags = new int[N];
                for (int i = 0; i < N; i++) {
                    final String perm = p.requestedPermissions.get(i);
                    pi.requestedPermissions[i] = perm;
                    




                }
            }
        }
        PackageInfo base = null;
        try {
            base = VcloudCore.getContext().getPackageManager().getPackageInfo(p.packageName, semParaTodosUsuarios(flags));
        } catch (PackageManager.NameNotFoundException ignored) {
        } catch (Throwable t) {
            // o PackageInfo do Vcloud não pode depender de a consulta ao celular dar certo
            com.vcloud.motor.utils.Diagnostico.eventoLimitado("pkginfobase|" + p.packageName, 30_000,
                    "PACOTE consulta ao celular de " + p.packageName + " falhou (flags=" + flags + "): " + t);
        }
        try {
        if ((flags & PackageManager.GET_SIGNATURES) != 0) {
            if (base == null) {
                pi.signatures = p.mSignatures;
                // Vcloud: apps assinados só com v2/v3 (a maioria dos jogos) ficam sem `mSignatures`; as assinaturas estão em mSigningDetails.
                // A Play Store só conta como "instalado" (e mostra Desinstalar/Abrir) o app que tem assinatura em PackageInfo.signatures.
                if ((pi.signatures == null || pi.signatures.length == 0) && p.mSigningDetails != null) {
                    pi.signatures = p.mSigningDetails.signatures;
                }
            } else {
                pi.signatures = base.signatures;
            }
        }
        if (BuildCompat.isPie()) {
            if ((flags & PackageManager.GET_SIGNING_CERTIFICATES) != 0) {
                if (base == null) {
                    PackageParser.SigningDetails signingDetails = PackageParser.SigningDetails.UNKNOWN;
                    BRPackageParserSigningDetails.get(signingDetails)._set_signatures(p.mSigningDetails.signatures);
                    pi.signingInfo = BRSigningInfo.get()._new(signingDetails);
                } else {
                    pi.signingInfo = base.signingInfo;
                }
            }
        }
        } catch (Throwable t) {
            // Vcloud: falha ao montar a assinatura não pode apagar o PackageInfo inteiro
            com.vcloud.motor.utils.Diagnostico.eventoLimitado("pkginfosig|" + p.packageName, 30_000,
                    "PACOTE assinatura de " + p.packageName + " não montada (flags=" + flags + "): " + t);
        }
        if ("com.android.vending".equals(p.packageName) || "com.google.android.gms".equals(p.packageName)) {
            // Vcloud: registra a assinatura entregue (jogos pagos conferem se a Play Store é original)
            android.util.Log.i("VcloudAssinatura", p.packageName + " flags=" + flags + " doCelular=" + (base != null)
                    + " signatures=" + (pi.signatures == null ? "null" : pi.signatures.length)
                    + " signingInfo=" + (BuildCompat.isPie() ? (pi.signingInfo != null) : "n/a"));
        }
        return pi;
    }

    public static ActivityInfo generateActivityInfo(BPackage.Activity a, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, a.info.applicationInfo)) {
            return null;
        }
        
        ActivityInfo ai = new ActivityInfo(a.info);
        ai.metaData = a.metaData;
        ai.processName = BPackageManagerService.fixProcessName(ai.packageName, ai.processName);
        ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
        return ai;
    }

    public static ServiceInfo generateServiceInfo(BPackage.Service s, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, s.info.applicationInfo)) {
            return null;
        }
        
        ServiceInfo si = new ServiceInfo(s.info);
        si.metaData = s.metaData;
        si.processName = BPackageManagerService.fixProcessName(si.packageName, si.processName);
        si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
        return si;
    }

    public static ProviderInfo generateProviderInfo(BPackage.Provider p, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.info.applicationInfo)) {
            return null;
        }
        
        ProviderInfo pi = new ProviderInfo(p.info);
        if (pi.authority == null)
            return null;
        pi.metaData = p.metaData;
        pi.processName = BPackageManagerService.fixProcessName(pi.packageName, pi.processName);
        if ((flags & PackageManager.GET_URI_PERMISSION_PATTERNS) == 0) {
            pi.uriPermissionPatterns = null;
        }
        pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
        return pi;
    }

    public static PermissionInfo generatePermissionInfo(
            BPackage.Permission p, int flags) {
        if (p == null) return null;
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static InstrumentationInfo generateInstrumentationInfo(
            BPackage.Instrumentation i, int flags) {
        if (i == null) return null;
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    /**
     * Vcloud: partes (splits) do app instaladas pela Play Store (recursos, assets, pacotes de dados como UnityDataAssetPack) ficam em
     * data/app/<pacote>/splits/<nome>.apk. Entram no ApplicationInfo como splitSourceDirs/splitNames, e o Android monta os recursos e os
     * assets do app com elas (sem isso o jogo abria mas faltavam arquivos: "RemoteProviderException ... unable to load").
     */
    private static void adicionarPartes(ApplicationInfo ai, String pacote) {
        try {
            java.io.File[] fs = new java.io.File(BEnvironment.getAppDir(pacote), "splits").listFiles();
            if (fs == null || fs.length == 0) return;
            java.util.Arrays.sort(fs);
            java.util.List<String> caminhos = new java.util.ArrayList<>(), nomes = new java.util.ArrayList<>();
            for (java.io.File f : fs) {
                if (!f.getName().endsWith(".apk")) continue;
                caminhos.add(f.getAbsolutePath());
                nomes.add(f.getName().substring(0, f.getName().length() - 4));
            }
            if (caminhos.isEmpty()) return;
            String[] c = caminhos.toArray(new String[0]);
            com.vcloud.motor.utils.Reflector.with(ai).field("splitSourceDirs").set(c);
            com.vcloud.motor.utils.Reflector.with(ai).field("splitPublicSourceDirs").set(c);
            com.vcloud.motor.utils.Reflector.with(ai).field("splitNames").set(nomes.toArray(new String[0]));
        } catch (Throwable ignored) { }
    }

    public static ApplicationInfo generateApplicationInfo(BPackage p, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo)) {
            return null;
        }
        ApplicationInfo baseApplication;
        try {
            baseApplication = VcloudCore.getPackageManager().getApplicationInfo(VcloudCore.getHostPkg(), semParaTodosUsuarios(flags));
        } catch (Exception e) {
            return null;
        }
        String sourceDir = p.baseCodePath;
        if (p.applicationInfo == null) {
            p.applicationInfo = VcloudCore.getPackageManager()
                    .getPackageArchiveInfo(sourceDir, 0).applicationInfo;
        }
        ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
        if ((flags & PackageManager.GET_META_DATA) != 0) {
            ai.metaData = p.mAppMetaData;
        }
        ai.dataDir = BEnvironment.getDataDir(ai.packageName, userId).getAbsolutePath();
        if (!p.installOption.isFlag(InstallOption.FLAG_SYSTEM)) {
            ai.nativeLibraryDir = BEnvironment.getAppLibDir(ai.packageName).getAbsolutePath();
        }
        ai.processName = BPackageManagerService.fixProcessName(p.packageName, ai.packageName);
        ai.publicSourceDir = sourceDir;
        ai.sourceDir = sourceDir;
        ai.uid = p.mExtras.appId;
        adicionarPartes(ai, p.packageName);


        if (BuildCompat.isL()) {
            BRApplicationInfoL.get(ai)._set_primaryCpuAbi(Build.CPU_ABI);
            BRApplicationInfoL.get(ai)._set_scanPublicSourceDir(BRApplicationInfoL.get(baseApplication).scanPublicSourceDir());
            BRApplicationInfoL.get(ai)._set_scanSourceDir(BRApplicationInfoL.get(baseApplication).scanSourceDir());
        }
        if (BuildCompat.isN()) {
            ai.deviceProtectedDataDir = BEnvironment.getDeDataDir(p.packageName, userId).getAbsolutePath();

            if (BRApplicationInfoN.get(ai)._check_deviceEncryptedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_deviceEncryptedDataDir(ai.deviceProtectedDataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_credentialEncryptedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_credentialEncryptedDataDir(ai.dataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_deviceProtectedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_deviceProtectedDataDir(ai.deviceProtectedDataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_credentialProtectedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_credentialProtectedDataDir(ai.dataDir);
            }
        }
        fixJar(ai);
        return ai;
    }

    private static boolean checkUseInstalledOrHidden(int flags, BPackageUserState state,
                                                     ApplicationInfo appInfo) {
        if (AppSystemEnv.isBlackPackage(appInfo.packageName))
            return false;
        
        if (!state.installed || state.hidden) {
            return false;
        }
        return true;
    }

    private static void fixJar(ApplicationInfo info) {
        String APACHE_LEGACY_JAR = "/system/framework/org.apache.http.legacy.boot.jar";
        String APACHE_LEGACY_JAR_Q = "/system/framework/org.apache.http.legacy.jar";
        Set<String> sharedLibraryFileList = new HashSet<>();
        if (BuildCompat.isQ()) {
            if (!FileUtils.isExist(APACHE_LEGACY_JAR_Q)) {
                sharedLibraryFileList.add(APACHE_LEGACY_JAR);
            } else {
                sharedLibraryFileList.add(APACHE_LEGACY_JAR_Q);
            }
        } else {
            sharedLibraryFileList.add(APACHE_LEGACY_JAR);
        }





        info.sharedLibraryFiles = sharedLibraryFileList.toArray(new String[]{});
    }

    public static Resources getResources(Context context, ApplicationInfo appInfo) {
        BPackageSettings ps = BPackageManagerService.get().getBPackageSetting(appInfo.packageName);
        if (ps != null) {
            AssetManager assets = BRAssetManager.get()._new();
            BRAssetManager.get(assets).addAssetPath(ps.pkg.baseCodePath);
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        return null;
    }
}
