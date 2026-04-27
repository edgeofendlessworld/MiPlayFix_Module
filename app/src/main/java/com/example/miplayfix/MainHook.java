package com.example.miplayfix;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    public MainHook(@NonNull XposedInterface base) {
        super(base);
    }

    @Override
    public void onPackageLoaded(
            @NonNull XposedModuleInterface.PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName()))
            return;

        log("MiPlayFix: injected -> " + TARGET_PACKAGE);

        try {

            ClassLoader cl = param.getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            method.setAccessible(true);

            base.hook(method, MyHooker.class);

        } catch (Throwable e) {

            log("MiPlayFix hook failed: " + e);

        }
    }

    @XposedHooker
    public static class MyHooker {

        @BeforeInvocation
        public static void before(
                XposedInterface.BeforeHookCallback callback
        ) {

            Object[] args = callback.getArgs();

            int original = (int) args[1];

            int newDelay = 50000;

            args[1] = newDelay;

        }
    }
}