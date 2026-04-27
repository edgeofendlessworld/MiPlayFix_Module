package com.example.miplayfix;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    @Override
    public void onLoadedPackage(Object param) {

        String pkg = getPackageName(param);
        if (!TARGET_PACKAGE.equals(pkg)) return;

        log(0, "MiPlayFix", "Injected -> " + pkg);

        try {
            ClassLoader cl = getClassLoader(param);

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(method);

        } catch (Throwable t) {
            log(1, "MiPlayFix", "hook failed", t);
        }
    }

    @Override
    public void handleHook(Object[] args) {

        int original = (int) args[1];

        args[1] = 50000;
    }

    // ===== 兼容 =====

    private String getPackageName(Object param) {
        try {
            return (String) param.getClass()
                    .getMethod("getPackageName")
                    .invoke(param);
        } catch (Throwable t) {
            return "";
        }
    }

    private ClassLoader getClassLoader(Object param) {
        try {
            return (ClassLoader) param.getClass()
                    .getMethod("getClassLoader")
                    .invoke(param);
        } catch (Throwable t) {
            return getClass().getClassLoader();
        }
    }
}