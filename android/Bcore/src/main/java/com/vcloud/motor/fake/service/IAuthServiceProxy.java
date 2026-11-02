package com.vcloud.motor.fake.service;

/** BiometricPrompt (serviço "auth"). */
public class IAuthServiceProxy extends PacoteDoVcloudProxy {
    public IAuthServiceProxy() { super("auth", "android.hardware.biometrics.IAuthService$Stub"); }
}
