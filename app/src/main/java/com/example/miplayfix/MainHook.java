package com.example.miplayfix;

import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.lang.reflect.Method;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    public MainHook(@NonNull NativePlaceHolder base, @NonNull ModuleContext context) {
        super(base, context);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageContext context) {
        // 过滤目标包名
        if (!context.getPackageName().equals(TARGET_PACKAGE)) return;

        log("MiPlayFix: 成功注入目标应用 -> " + TARGET_PACKAGE);

        try {
            // 在目标 ClassLoader 中查找方法
            Class<?> targetClazz = context.getApplicationInfo().classLoader.loadClass(TARGET_CLASS);
            Method targetMethod = targetClazz.getDeclaredMethod(TARGET_METHOD, long.class, int.class);

            // 执行 Hook
            hookMethod(targetMethod, MyHooker.class);
        } catch (Exception e) {
            log("MiPlayFix: 注入失败 -> " + e.getMessage());
        }
    }

    @XposedHooker
    public static class MyHooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            Object[] args = callback.getArgs();
            int originalDelay = (int) args[1];
            
            // 修改延迟值为 50000
            int newDelay = 50000;
            args[1] = newDelay;
            
            // 记录日志（需通过回调或静态引用）
            // XposedInterface.log("MiPlayFix: 修改延迟 [ " + originalDelay + " -> " + newDelay + " ]");
        }
    }
}