package com.xposed.miplayfix;

import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * MiPlayFix - 小米投屏服务音频延迟修复模块
 * 基于 libxposed API 101.0.0
 */
public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";
    private static final int NEW_DELAY = 50000; // 延迟时间，单位为微秒，默认为50ms
    private static final String TAG = "MiPlayFix";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log("模块已加载");
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!param.getPackageName().equals(TARGET_PACKAGE)) {
            return;
        }

        log("成功注入目标应用 -> " + TARGET_PACKAGE);

        try {
            hookAudioDelayMethod(param);
        } catch (Throwable e) {
            log("注入失败 -> " + e.getMessage());
        }
    }

    private void hookAudioDelayMethod(XposedModuleInterface.PackageLoadedParam param) {
        try {
            ClassLoader classLoader = param.getDefaultClassLoader();
            Class<?> targetClass = Class.forName(TARGET_CLASS, false, classLoader);

            java.lang.reflect.Method targetMethod = targetClass.getDeclaredMethod(
                TARGET_METHOD,
                long.class,
                int.class
            );

            hook(targetMethod).intercept(chain -> {
                try {
                    Object[] args = chain.getArgs().toArray();
                    int originalDelay = (int) args[1];
                    
                    args[1] = NEW_DELAY;
                    
                    log("音频延迟已修改 [ " + originalDelay + " -> " + NEW_DELAY + " ]");
                    
                    return chain.proceed(args);
                } catch (Exception e) {
                    log("参数修改失败: " + e.getMessage());
                    return chain.proceed();
                }
            });

        } catch (ClassNotFoundException e) {
            log("未找到目标类: " + TARGET_CLASS);
        } catch (NoSuchMethodException e) {
            log("未找到目标方法: " + TARGET_METHOD);
        }
    }

    private void log(String message) {
        log(Log.INFO, TAG, message);
    }
}
