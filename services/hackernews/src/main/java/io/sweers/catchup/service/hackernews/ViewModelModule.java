package io.sweers.catchup.service.hackernews;

import androidx.lifecycle.ViewModel;

import com.squareup.inject.assisted.dagger2.AssistedModule;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import io.sweers.catchup.service.hackernews.viewmodelbits.ViewModelAssistedFactory;
import io.sweers.catchup.service.hackernews.viewmodelbits.ViewModelKey;

@AssistedModule
@Module(includes = AssistedInject_ViewModelModule.class)
public abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(HackerNewsCommentsViewModel.class)
    abstract ViewModelAssistedFactory<? extends ViewModel> mainViewModel(HackerNewsCommentsViewModel.Factory viewModel);
}