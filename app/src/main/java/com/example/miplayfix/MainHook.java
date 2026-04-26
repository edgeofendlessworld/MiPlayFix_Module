package com.example.miplayfix;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedInterface.PackageLoadedParam;
import io.github.libxposed.api.XposedInterface.MethodHookParam;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    public MainHook() {
        super();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;

        log("MiPlayFix: hooked -> " + TARGET_PACKAGE);

        try {
            ClassLoader cl = param.getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            // API 101：Hooker + chain 模式
            hook(method, new Hooker() {
                @Override
                public Object intercept(Chain chain) throws Throwable {

                    MethodHookParam param = chain.getMethodHookParam();

                    Object[] args = param.getArgs();

                    // 修改参数
                    args[1] = 50000;

                    // 继续执行调用链（关键点）
                    return chain.proceed(param.getThisObject(), args);
                }
            });

        } catch (Throwable e) {
            log("MiPlayFix error: " + e);
        }
    }

    private void log(String msg) {
        android.util.Log.i("MiPlayFix", msg);
    }
}
