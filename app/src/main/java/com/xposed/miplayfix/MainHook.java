package com.xposed.miplayfix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log("MiPlayFix: 成功注入目标应用 -> " + TARGET_PACKAGE);

        try {
            Class<?> targetClass = XposedHelpers.findClass(
                TARGET_CLASS,
                lpparam.classLoader
            );
            
            XposedHelpers.findAndHookMethod(
                targetClass,
                TARGET_METHOD,
                long.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int originalDelay = (int) param.args[1];
                        int newDelay = 50000; // 延迟时间，单位为微秒，默认为50ms
                        param.args[1] = newDelay;
                        
                        XposedBridge.log("MiPlayFix: 音频延迟已修改 [ " + originalDelay + " -> " + newDelay + " ]");
                    }
                }
            );
        } catch (Throwable e) {
            XposedBridge.log("MiPlayFix: 注入失败 -> " + e.getMessage());
        }
    }
}