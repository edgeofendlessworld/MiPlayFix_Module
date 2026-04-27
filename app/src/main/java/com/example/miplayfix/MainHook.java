package com.example.miplayfix;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.hooker.MethodHooker;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";

    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";

    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName()))
            return;

        log(INFO, "MiPlayFix", "injected");

        try {
            ClassLoader cl = param.getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(method, DelayHooker.class);

        } catch (Throwable e) {
            log(ERROR, "MiPlayFix", "hook failed", e);
        }
    }

    public static class DelayHooker implements MethodHooker {

        @Override
        public void before(MethodHookParam param) {

            try {
                Object[] args = param.args;

                int original = (int) args[1];

                int newDelay = 50000;

                args[1] = newDelay;

                log(INFO, "MiPlayFix",
                        "delay " + original + " -> " + newDelay);

            } catch (Throwable e) {
                log(ERROR, "MiPlayFix", "before hook error", e);
            }
        }
    }
}