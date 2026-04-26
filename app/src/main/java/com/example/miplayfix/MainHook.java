package com.example.miplayfix;

import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

public class MainHook extends XposedModule {

    private static final String TAG = "MiPlayFix";

    private static final String TARGET_PACKAGE =
            "com.milink.service";

    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";

    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    public MainHook(XposedInterface base,
                    XposedModuleInterface.ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;

        log(Log.INFO, TAG, "已注入目标应用: " + TARGET_PACKAGE);

        try {
            ClassLoader cl = param.getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(method)
                    .intercept(chain -> {

                        Object[] args = chain.getArgs();

                        // 原参数
                        int originalDelay = (int) args[1];

                        // 修改值
                        int newDelay = 50000;

                        args[1] = newDelay;

                        log(Log.INFO, TAG,
                                "Hook成功: " + originalDelay + " -> " + newDelay);

                        return chain.proceed(args);
                    });

        } catch (Throwable e) {
            log(Log.ERROR, TAG, "注入失败: " + e);
        }
    }
}