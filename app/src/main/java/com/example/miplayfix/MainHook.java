package com.example.miplayfix;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.*;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    public MainHook(@NonNull NativePlaceHolder base,
                    @NonNull ModuleContext context) {
        super(base, context);
    }

    @Override
    public void onPackageReady(@NonNull PackageContext context) {

        // 过滤目标包名
        if (!context.getPackageName().equals(TARGET_PACKAGE)) return;

        logI("MiPlayFix: 成功注入目标应用 -> " + TARGET_PACKAGE);

        try {

            ClassLoader cl = context.getClassLoader();

            Class<?> targetClazz =
                    cl.loadClass(TARGET_CLASS);

            Method targetMethod =
                    targetClazz.getDeclaredMethod(
                            TARGET_METHOD,
                            long.class,
                            int.class
                    );

            targetMethod.setAccessible(true);

            // 使用标准 hook API
            xposed.hook(targetMethod, MyHooker.class);

            logI("MiPlayFix: hook 成功");

        } catch (Throwable e) {

            logE("MiPlayFix: 注入失败", e);

        }
    }

    private void log(String msg) {
        android.util.Log.i("MiPlayFix", msg);
    }
}