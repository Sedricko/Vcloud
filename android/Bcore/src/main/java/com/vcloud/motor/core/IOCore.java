package com.vcloud.motor.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.system.Os;
import android.util.Log;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vcloud.motor.VcloudCore;

import com.vcloud.motor.core.env.BEnvironment;
import com.vcloud.motor.utils.FileUtils;
import com.vcloud.motor.utils.TrieTree;


@SuppressLint("SdCardPath")
public class IOCore {
    public static final String TAG = "IOCore";

    private static final IOCore sIOCore = new IOCore();
    private static final TrieTree mTrieTree = new TrieTree();
    private static final TrieTree sBlackTree = new TrieTree();
    private final Map<String, String> mRedirectMap = new LinkedHashMap<>();

    private static final Map<String, Map<String, String>> sCachePackageRedirect = new HashMap<>();

    public static IOCore get() {
        return sIOCore;
    }

    
    public void addRedirect(String origPath, String redirectPath) {
        if (TextUtils.isEmpty(origPath) || TextUtils.isEmpty(redirectPath) || mRedirectMap.get(origPath) != null)
            return;
        
        mTrieTree.add(origPath);
        mRedirectMap.put(origPath, redirectPath);
        File redirectFile = new File(redirectPath);
        if (!redirectFile.exists()) {
            FileUtils.mkdirs(redirectPath);
        }
        NativeCore.addIORule(origPath, redirectPath);
    }

    public void addBlackRedirect(String path) {
        if (TextUtils.isEmpty(path))
            return;
        sBlackTree.add(path);
    }

    public String redirectPath(String path) {
        if (TextUtils.isEmpty(path))
            return path;
        if (path.contains("/blackbox/")) {
            return path;
        }
        String search = sBlackTree.search(path);
        if (!TextUtils.isEmpty(search))
            return search;

        
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(mRedirectMap.get(key)));

        return path;
    }

    public File redirectPath(File path) {
        if (path == null)
            return null;
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr));
    }

    public String redirectPath(String path, Map<String, String> rule) {
        if (TextUtils.isEmpty(path))
            return path;

        
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(rule.get(key)));

        return path;
    }

    public File redirectPath(File path, Map<String, String> rule) {
        if (path == null)
            return null;
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr, rule));
    }

    

    public void enableRedirect(Context context) {
        Map<String, String> rule = new LinkedHashMap<>();
        Set<String> blackRule = new HashSet<>();
        String packageName = context.getPackageName();

        try {
            ApplicationInfo packageInfo = VcloudCore.getBPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA, VcloudCore.getUserId());
            int systemUserId = VcloudCore.getHostUserId();
            rule.put(String.format("/data/data/%s/lib", packageName), packageInfo.nativeLibraryDir);
            rule.put(String.format("/data/user/%d/%s/lib", systemUserId, packageName), packageInfo.nativeLibraryDir);

            rule.put(String.format("/data/data/%s", packageName), packageInfo.dataDir);
            rule.put(String.format("/data/user/%d/%s", systemUserId, packageName), packageInfo.dataDir);

            
            File profilesRoot = new File(BEnvironment.getVirtualRoot(), "profiles");
            FileUtils.mkdirs(profilesRoot.getAbsolutePath());
            
            rule.put("/data/misc/profiles", profilesRoot.getAbsolutePath());

            File profilesCurDir = new File(profilesRoot, String.format("cur/%d/%s", VcloudCore.getUserId(), packageName));
            File profilesRefDir = new File(profilesRoot, String.format("ref/%d/%s", VcloudCore.getUserId(), packageName));
            FileUtils.mkdirs(profilesCurDir.getAbsolutePath());
            FileUtils.mkdirs(profilesRefDir.getAbsolutePath());
            rule.put(String.format("/data/misc/profiles/cur/%d/%s", VcloudCore.getUserId(), packageName), profilesCurDir.getAbsolutePath());
            rule.put(String.format("/data/misc/profiles/ref/%d/%s", VcloudCore.getUserId(), packageName), profilesRefDir.getAbsolutePath());

            if (VcloudCore.getContext().getExternalCacheDir() != null && context.getExternalCacheDir() != null) {
                File external = BEnvironment.getExternalUserDir(VcloudCore.getUserId());

                
                rule.put("/sdcard", external.getAbsolutePath());
                rule.put(String.format("/storage/emulated/%d", systemUserId), external.getAbsolutePath());

                blackRule.add("/sdcard/Pictures");
                blackRule.add(String.format("/storage/emulated/%d/Pictures", systemUserId));
            }
            if (VcloudCore.get().isHideRoot()) {
                hideRoot(rule);
            }
            liberarRoot(rule, packageName);
            proc(rule);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String key : rule.keySet()) {
            get().addRedirect(key, rule.get(key));
        }
        for (String s : blackRule) {
            get().addBlackRedirect(s);
        }
        NativeCore.enableIO();
    }

    private static final String[] SU_PATHS = {"/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};

    private void hideRoot(Map<String, String> rule) {
        rule.put("/system/app/Superuser.apk", "/system/app/Superuser.apk-fake");
        for (String p : SU_PATHS) rule.put(p, p + "-fake");
    }

    // Root interno: "su" é um atalho para o libvsu.so do Vcloud, só no PATH dos apps de dentro. Quem pode usar é decidido
    // pelo painel (RootPolicy) a cada pedido, dentro do próprio su. Não é root de verdade: vale só no Vcloud.
    // shortcut: exec por caminho absoluto (/system/xbin/su) não é redirecionado; subir para hook de execve se algum app exigir.
    private void liberarRoot(Map<String, String> rule, String packageName) {
        try {
            if (!com.vcloud.motor.core.root.RootPolicy.padrao().geralLigado()) return;
            File lib = new File(VcloudCore.getContext().getApplicationInfo().nativeLibraryDir, "libvsu.so");
            if (!lib.exists()) {
                com.vcloud.motor.utils.Diagnostico.evento("ROOT indisponível: " + lib + " não está no APK");
                return;
            }
            File bin = new File(BEnvironment.getVirtualRoot(), "rootbin");
            FileUtils.mkdirs(bin.getAbsolutePath());
            File su = new File(bin, "su");
            // O caminho da pasta de bibliotecas muda a cada atualização do APK: refaz o atalho sempre.
            su.delete();
            try {
                Os.symlink(lib.getAbsolutePath(), su.getAbsolutePath());
            } catch (android.system.ErrnoException e) {
                if (e.errno != android.system.OsConstants.EEXIST) throw e; // outro app abriu junto e já refez
            }
            Os.setenv("PATH", bin.getAbsolutePath() + ":" + System.getenv("PATH"), true);
            Os.setenv("VCLOUD_PKG", packageName, true); // só uma dica: o servidor identifica o app pelo processo
            for (String p : SU_PATHS) rule.put(p, su.getAbsolutePath());
            Log.i("VcloudRoot", "su pronto para " + packageName);
        } catch (Throwable e) {
            Log.e("VcloudRoot", "falhou ao preparar o su", e);
            com.vcloud.motor.utils.Diagnostico.evento("ROOT falhou ao preparar o su para " + packageName + ": " + e);
        }
    }

    private void proc(Map<String, String> rule) {
        int appPid = VcloudCore.getAppPid();
        int pid = Process.myPid();
        String selfProc = "/proc/self/";
        String proc = "/proc/" + pid + "/";

        String cmdline = new File(BEnvironment.getProcDir(appPid), "cmdline").getAbsolutePath();
        rule.put(proc + "cmdline", cmdline);
        rule.put(selfProc + "cmdline", cmdline);
    }
}
