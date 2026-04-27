package com.example.miplayfix;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.PackageLoadedParam;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;

        log("MiPlayFix injected -> " + TARGET_PACKAGE);

        try {
            ClassLoader cl = param.getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(method, MyHooker.class);

        } catch (Throwable t) {
            log("hook failed: " + t);
        }
    }

    public static class MyHooker implements XposedInterface.Hooker {

        public static void before(XposedInterface.BeforeHookCallback callback) {

            Object[] args = callback.getArgs();

            int original = (int) args[1];

            args[1] = 50000;

        }
    }
}
