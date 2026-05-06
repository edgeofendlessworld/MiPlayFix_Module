package com.xposed.miplayfix;

import android.util.Log;

import androidx.annotation.NonNull;

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
    private static final int NEW_DELAY = 50000; // 微秒，即50ms
    private static final String TAG = "MiPlayFix";

    @Override
    public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        log("模块已加载");
    }

    @Override
    public void onPackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        // 仅处理目标包
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

    /**
     * Hook 音频延迟方法
     */
    private void hookAudioDelayMethod(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        try {
            ClassLoader classLoader = param.getDefaultClassLoader();
            Class<?> targetClass = Class.forName(TARGET_CLASS, false, classLoader);

            java.lang.reflect.Method targetMethod = targetClass.getDeclaredMethod(
                TARGET_METHOD,
                long.class,
                int.class
            );

            // 使用 intercept() 方法，而不是 before()
            hook(targetMethod).intercept(chain -> {
                try {
                    int originalDelay = (int) chain.getArgs().get(1);
                    chain.getArgs().set(1, NEW_DELAY);
                    log("音频延迟已修改 [ " + originalDelay + " -> " + NEW_DELAY + " ]");
                } catch (Exception e) {
                    log("参数修改失败: " + e.getMessage());
                }
                // 继续执行原始方法
                return chain.proceed();
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
