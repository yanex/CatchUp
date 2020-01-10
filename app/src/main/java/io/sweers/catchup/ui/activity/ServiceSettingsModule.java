package io.sweers.catchup.ui.activity;

import java.util.Map;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import dagger.multibindings.Multibinds;
import io.sweers.catchup.injection.scopes.PerFragment;
import io.sweers.catchup.service.api.ServiceMeta;
import io.sweers.catchup.ui.activity.ServiceSettingsActivity.ServiceSettingsFrag;
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceMetaRegistry;

@Module(includes = ResolvedCatchUpServiceMetaRegistry.class)
public abstract class ServiceSettingsModule {
    @Multibinds
    public abstract Map<String, ServiceMeta> serviceMetas();

    @PerFragment
    @ContributesAndroidInjector
    public abstract ServiceSettingsFrag serviceSettingsFragment();
}