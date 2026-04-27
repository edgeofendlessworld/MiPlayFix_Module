package com.example.miplayfix;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(XposedInterface.PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.packageName))
            return;

        log("MiPlayFix injected");

        try {

            ClassLoader cl = param.classLoader;

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            method.setAccessible(true);

            hook(method, callback -> {

                Object[] args = callback.args;

                int original = (int) args[1];

                int newDelay = 50000;

                args[1] = newDelay;

                log("delay " + original + " -> " + newDelay);

            });

        } catch (Throwable e) {

            log("MiPlayFix hook failed: " + e);

        }
    }
}