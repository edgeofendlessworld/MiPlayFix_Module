package com.example.miplayfix;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.PackageLoadedParam;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedInterface.BeforeHookCallback;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    public MainHook(@NonNull Object base) {
        super(base);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {

        if (!param.getPackageName().equals(TARGET_PACKAGE)) return;

        log("MiPlayFix: 成功注入目标应用 -> " + TARGET_PACKAGE);

        try {
            Class<?> clazz = param.getClassLoader().loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            param.hook(method, MyHooker.class);

        } catch (Throwable e) {
            log("MiPlayFix: 注入失败 " + e);
        }
    }

    public static class MyHooker implements Hooker {

        public static void before(BeforeHookCallback callback) {

            Object[] args = callback.getArgs();

            // int -> 直接安全转换
            int original = (int) args[1];

            int newDelay = 50000;

            args[1] = newDelay;
        }
    }

    private void log(String msg) {
        android.util.Log.i("MiPlayFix", msg);
    }
}