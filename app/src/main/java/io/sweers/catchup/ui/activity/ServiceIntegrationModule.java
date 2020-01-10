package io.sweers.catchup.ui.activity;

import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import dagger.Binds;
import dagger.Provides;
import dagger.multibindings.Multibinds;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.ServiceDao;
import io.sweers.catchup.injection.ActivityModule;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.service.api.LinkHandler;
import io.sweers.catchup.service.api.Service;
import io.sweers.catchup.service.api.ServiceMeta;
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceRegistry;
import io.sweers.catchup.ui.DetailDisplayer;
import io.sweers.catchup.ui.fragments.service.StorageBackedService;

@dagger.Module(includes = ResolvedCatchUpServiceRegistry.class)
public abstract class ServiceIntegrationModule implements ActivityModule<MainActivity> {
    @TextViewPool
    @Provides
    @PerActivity
    public static RecycledViewPool provideTextViewPool() {
        return new RecycledViewPool();
    }

    @VisualViewPool
    @Provides
    @PerActivity
    public static RecycledViewPool provideVisualViewPool() {
        return new RecycledViewPool();
    }

    @Provides
    @PerActivity
    @FinalServices
    public static Map<String, Provider<Service>> provideFinalServices(
            ServiceDao serviceDao,
            Map<String, ServiceMeta> serviceMetas,
            SharedPreferences sharedPreferences,
            Map<String, Provider<Service>> services
    ) {
        Map<String, Provider<Service>> result = new LinkedHashMap<>();

        for (Map.Entry<String, Provider<Service>> service : services.entrySet()) {
            ServiceMeta serviceMeta = serviceMetas.get(service.getKey());
            assert serviceMeta != null;
            String enabledPref = serviceMeta.getEnabledPreferenceKey();
            if (serviceMeta.getEnabled() && sharedPreferences.getBoolean(enabledPref, true)) {
                result.put(service.getKey(), () -> new StorageBackedService(serviceDao, service.getValue().get()));
            }
        }

        return result;
    }

    @Multibinds
    public abstract Map<String, Service> services();

    @Multibinds
    public abstract Map<String, ServiceMeta> serviceMetas();

    @Multibinds
    public abstract Map<Class<? extends Fragment>, Fragment> fragmentCreators();

    @Binds
    @PerActivity
    public abstract LinkHandler provideLinkHandler(LinkManager linkManager);

    @Binds
    @PerActivity
    public abstract DetailDisplayer provideDetailDisplayer(
            MainActivityDetailDisplayer mainActivityDetailDisplayer
    );

    @Binds
    @PerActivity
    public abstract FragmentFactory provideFragmentFactory(
            MainActivityFragmentFactory fragmentFactory
    );
}