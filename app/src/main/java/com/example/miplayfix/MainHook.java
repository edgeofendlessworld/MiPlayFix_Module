package com.example.miplayfix;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface.PackageLoadedParam;
import io.github.libxposed.api.Hooker;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {

        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;

        try {
            log("MiPlayFix injected: " + TARGET_PACKAGE);

            ClassLoader cl = param.classLoader; // ✅ 正确方式（不是 getClassLoader）

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            param.hook(method, MyHooker.class);

        } catch (Throwable t) {
            log("hook failed: " + t);
        }
    }

    public static class MyHooker implements Hooker {

        @Override
        public Object intercept(Chain<Object> chain) throws Throwable {

            List<Object> args = chain.getArgs();

            // 第二个参数 int delay
            int original = (int) args.get(1);

            args.set(1, 50000); // 修改延迟

            return chain.proceed(args);
        }
    }

    private void log(String msg) {
        android.util.Log.i("MiPlayFix", msg);
    }
}