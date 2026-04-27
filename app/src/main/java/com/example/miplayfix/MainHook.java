package com.example.miplayfix;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(XposedInterface.PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;

        log(0, "MiPlayFix", "Injected -> " + TARGET_PACKAGE);

        try {
            ClassLoader cl = param.getClass().getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook((Executable) method, new HookImpl());

        } catch (Throwable t) {
            log(1, "MiPlayFix", "hook failed", t);
        }
    }

    public static class HookImpl implements XposedInterface.Hooker {

        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {

            List<Object> args = chain.getArgs();

            int original = (int) args.get(1);

            args.set(1, 50000);

            return chain.proceed(args);
        }
    }
}
