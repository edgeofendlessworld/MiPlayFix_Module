package com.example.miplayfix;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Chain;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(Object param) {

        // ⚠️ 这里不再依赖 PackageLoadedParam（你这个版本没有）
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

            hook((Executable) method, new HookImpl());

        } catch (Throwable t) {
            log(1, "MiPlayFix", "hook failed", t);
        }
    }

    public static class HookImpl implements XposedInterface.Hooker {

        @Override
        public Object intercept(Chain chain) throws Throwable {

            Object[] args = (Object[]) chain.getArgs();

            int original = (int) args[1];

            args[1] = 50000;

            return chain.proceed(args);
        }
    }
}