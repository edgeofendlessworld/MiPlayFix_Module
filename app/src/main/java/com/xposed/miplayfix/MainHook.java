package com.xposed.miplayfix;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    public MainHook(XposedInterface base) {
        super(base);
    }

    @Override
    public void onPackageEvent(XposedInterface.PackageEvent event) throws Throwable {
        if (!event.packageName.equals("com.milink.service")) return;

        base.log("MiPlayFix: 成功注入目标应用 -> com.milink.service");

        try {
            var classLoader = event.classLoader;
            var targetClass = Class.forName(
                "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl",
                false,
                classLoader
            );

            base.hookMethod(
                targetClass.getDeclaredMethod("setAudioPlayDelayTime", long.class, int.class),
                new XposedInterface.MethodHook() {
                    @Override
                    public void beforeCall(XposedInterface.MethodHookParam param) throws Throwable {
                        int originalDelay = (int) param.args[1];
                        int newDelay = 50000; // 延迟时间，单位为微秒，默认为50ms
                        param.args[1] = newDelay;

                        base.log("MiPlayFix: 音频延迟已修改 [ " + originalDelay + " -> " + newDelay + " ]");
                    }
                }
            );
        } catch (Throwable e) {
            base.log("MiPlayFix: 注入失败 -> " + e.getMessage());
        }
    }
}