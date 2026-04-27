package com.example.miplayfix;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS   = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD  = "setAudioPlayDelayTime";

    @Override
    public void onPackageLoaded(XposedInterface.PackageLoadedParam param) throws Throwable {
        // 仅处理目标包
        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;
        // 日志：成功注入目标应用
        log(0, "MiPlayFix", "成功注入目标应用 -> " + TARGET_PACKAGE);

        try {
            // 通过 ClassLoader 反射获取目标类和方法
            ClassLoader cl = param.getClassLoader();
            Class<?> clazz = cl.loadClass(TARGET_CLASS);
            Method method = clazz.getDeclaredMethod(TARGET_METHOD, long.class, int.class);
            // 注册钩子：转换为 Executable 并传入 Hooker 实例
            hook((Executable) method, new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    // 获取并修改参数列表
                    List<Object> args = chain.getArgs();
                    int originalDelay = (int) args.get(1);
                    int newDelay = 50000;
                    args.set(1, newDelay);
                    // 日志：修改前后值
                    log(0, "MiPlayFix", "音频延迟已修改 [ " + originalDelay + " -> " + newDelay + " ]");
                    // 继续执行原方法
                    return chain.proceed(args);
                }
            });
        } catch (Throwable e) {
            log(1, "MiPlayFix", "注入失败 -> " + e.getMessage());
        }
    }
}
