package com.vcloud.motor.fake.service;

import java.lang.reflect.Method;

import com.vcloud.motor.utils.AttributionSourceUtils;

/**
 * Serviço "app_search" (AppSearch do Android): o gms de dentro cria a sessão com a origem do pedido (AttributionSource) no uid
 * virtual e o Android recusa ("Calling uid: 10454 doesn't match source uid: 10032", FacsCacheGmsModule). Aqui a origem vai com o
 * uid do celular e o nome do Vcloud, como nos provedores.
 */
public class IAppSearchProxy extends PacoteDoVcloudProxy {
    public IAppSearchProxy() { super("app_search", "android.app.appsearch.aidl.IAppSearchManager$Stub"); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        AttributionSourceUtils.fixAttributionSourceInArgs(args);
        return super.invoke(proxy, method, args);
    }
}
