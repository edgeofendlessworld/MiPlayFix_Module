package com.example.miplayfix;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD =
            "setAudioPlayDelayTime";


    public void start() {

        try {

            ClassLoader cl = getClass().getClassLoader();

            Class<?> clazz = cl.loadClass(TARGET_CLASS);

            Method method = clazz.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(method);

        } catch (Throwable t) {
            log(1, "MiPlayFix", "init failed", t);
        }
    }

    // hook 实际执行点（版本唯一稳定入口）
    public Object handleHook(Object[] args) {

        args[1] = 50000; //延迟时间，单位为微秒，默认为50ms
        return null;
    }
}