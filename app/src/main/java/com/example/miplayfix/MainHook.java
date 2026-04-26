package com.example.miplayfix;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

public class MainHook extends XposedModule {

    private static final String TAG = "MiPlayFix";

    private static final String TARGET_PACKAGE =
            "com.milink.service";

    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";

    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    // ✅ 101 正确构造函数（修复你第1个错误）
    public MainHook(XposedInterface base) {
        super(base);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;

        log("已注入: " + TARGET_PACKAGE);

        try {
            ClassLoader cl = param.getClassLoader(); // ✔ 修复点2（前提：正确 param 类型）

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(method)
                    .intercept(chain -> {

                        // ❗ 修复点3：getArgs() 返回的是 List<Object>
                        List<Object> args = chain.getArgs();

                        int original = (int) args.get(1);

                        int newDelay = 50000;

                        args.set(1, newDelay);

                        log("hook: " + original + " -> " + newDelay);

                        return chain.proceed(args);
                    });

        } catch (Throwable e) {
            log("错误: " + e);
        }
    }

    private void log(String msg) {
        Log.i(TAG, msg);
    }
}