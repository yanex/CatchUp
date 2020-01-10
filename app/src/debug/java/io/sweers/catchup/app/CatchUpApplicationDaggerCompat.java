package io.sweers.catchup.app;

public class CatchUpApplicationDaggerCompat {
    private CatchUpApplicationDaggerCompat() {}

    public static ApplicationComponent inject(CatchUpApplication app) {
        ApplicationComponent component = DaggerApplicationComponent.factory().create(app);
        component.inject(app);
        return component;
    }
}