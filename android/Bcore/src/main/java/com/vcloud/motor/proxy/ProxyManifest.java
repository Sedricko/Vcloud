package com.vcloud.motor.proxy;

import java.util.Locale;

import com.vcloud.motor.VcloudCore;


public class ProxyManifest {
    public static final int FREE_COUNT = 50;

    public static boolean isProxy(String msg) {
        return getBindProvider().equals(msg) || msg.contains("proxy_content_provider_");
    }

    public static String getBindProvider() {
        return VcloudCore.getHostPkg() + ".blackbox.SystemCallProvider";
    }

    public static String getProxyAuthorities(int index) {
        return String.format(Locale.CHINA, "%s.proxy_content_provider_%d", VcloudCore.getHostPkg(), index);
    }

    public static String getProxyPendingActivity(int index) {
        return String.format(Locale.CHINA, "com.vcloud.motor.proxy.ProxyPendingActivity$P%d", index);
    }

    public static String getProxyActivity(int index) {
        return String.format(Locale.CHINA, "com.vcloud.motor.proxy.ProxyActivity$P%d", index);
    }

    public static String TransparentProxyActivity(int index) {
        return String.format(Locale.CHINA, "com.vcloud.motor.proxy.TransparentProxyActivity$P%d", index);
    }

    public static String getProxyService(int index) {
        return String.format(Locale.CHINA, "com.vcloud.motor.proxy.ProxyService$P%d", index);
    }

    public static String getProxyJobService(int index) {
        return String.format(Locale.CHINA, "com.vcloud.motor.proxy.ProxyJobService$P%d", index);
    }

    public static String getProxyFileProvider() {
        return VcloudCore.getHostPkg() + ".blackbox.FileProvider";
    }

    public static String getProxyReceiver() {
        return VcloudCore.getHostPkg() + ".stub_receiver";
    }

    public static String getProcessName(int bPid) {
        return VcloudCore.getHostPkg() + ":p" + bPid;
    }
}
