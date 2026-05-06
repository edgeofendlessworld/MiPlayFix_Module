package com.example.miplaymod;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.MethodData;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayMod";
    // 目标 App 包名
    private static final String TARGET_PKG = "com.xiaomi.miplay"; 

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只 Hook 目标进程
        if (!lpparam.packageName.equals(TARGET_PKG)) return;

        // 异步执行，避免加载 Dex 时卡死主线程
        new Thread(() -> {
            try {
                // 等待一会儿确保 APK 已解压就绪
                Thread.sleep(2000); 
                
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    if (bridge == null) return;

                    // 1. 定位目标类和方法
                    // 注意：如果 smali 中的方法名不是 run，请修改此处
                    MethodData methodData = bridge.findMethod(
                        FindMethod.create()
                            .declaredClass("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                            .name("run") 
                    ).firstOrNull();

                    if (methodData != null) {
                        Log.d(TAG, "定位到目标方法: " + methodData.getDescriptor());

                        // 2. 修改字节码指令
                        methodData.getInstructions().forEach(ins -> {
                            // 查找 const-wide/16 vX, 2000 (0x7d0)
                            if (ins.getOpcode().name().contains("CONST_WIDE_16") && ins.getLiteral() == 2000L) {
                                // 将其修改为 0
                                ins.setLiteral(50L);
                                Log.d(TAG, "成功修改 0x7d0 为 0");
                            }
                        });

                        // 3. 保存修改
                        methodData.save();
                        Log.d(TAG, "修改已保存并生效");
                    } else {
                        Log.e(TAG, "未能找到目标方法，请确认方法名是否为 run");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Hook 过程中发生错误: ", e);
            }
        }).start();
    }
}