package com.example.miplayfix;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedInterface.PackageLoadedParam;
import io.github.libxposed.api.XposedInterface.XC_MethodHook;
import io.github.libxposed.api.XposedInterface.XC_MethodHook.MethodHookParam;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    // ⚠️ API 101：构造函数无参数
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

            hook(method, new Hooker() {
                @Override
                public Object intercept(XC_MethodHook.MethodHookParam param) throws Throwable {

                    Object[] args = param.args;

                    // 修改 delay
                    args[1] = 50000;

                    // 调用原方法
                    return param.getResult();
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