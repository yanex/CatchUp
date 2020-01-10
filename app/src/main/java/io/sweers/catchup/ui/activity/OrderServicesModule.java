package io.sweers.catchup.ui.activity;

import java.util.Map;

import dagger.Module;
import dagger.multibindings.Multibinds;
import io.sweers.catchup.service.api.ServiceMeta;
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceMetaRegistry;

@Module(includes = ResolvedCatchUpServiceMetaRegistry.class)
public abstract class OrderServicesModule {

    @Multibinds
    public abstract Map<String, ServiceMeta> serviceMetas();
}
