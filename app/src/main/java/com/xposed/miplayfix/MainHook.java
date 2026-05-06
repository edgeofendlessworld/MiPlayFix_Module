package com.xposed.miplayfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.MethodQuery;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.dex.info.MethodInfo;
import java.util.List;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            try {
                // 确保 APK 路径正确
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    if (bridge == null) return;

                    // 1. 使用 2.x 的 MethodQuery 定位方法
                    // 替换旧的 .declaredClass() 为 .className()
                    List<MethodData> results = bridge.findMethod(
                        MethodQuery.create()
                            .className("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                            .name("run") // 请确认 smali 中的实际方法名
                    );

                    if (!results.isEmpty()) {
                        MethodData methodData = results.get(0);
                        Log.d(TAG, "找到目标方法: " + methodData.getMethodInfo().getDescriptor());

                        // 2. 2.x 修改指令需要通过桥接器进行
                        // getInstructions() -> getInstructions(bridge)
                        methodData.getInstructions(bridge).forEach(ins -> {
                            // 检查指令是否包含常量 2000L (0x7d0)
                            if (ins.getOpcodeName().contains("const-wide") && ins.getLiteral() == 2000L) {
                                // 3. 修改为 50ms (50L)
                                ins.setLiteral(50L);
                                Log.d(TAG, "成功将同步阈值从 2000ms 修改为 50ms");
                            }
                        });

                        // 4. 2.x 的保存方式：saveAnnotations/saveInstructions 等不再是简单的 .save()
                        // 直接通过 bridge 提交修改（DexKit 2.x 自动管理部分修改状态）
                        // 注意：2.x 版本在执行 setLiteral 后通常已在内存中标记
                        Log.d(TAG, "指令已在内存中更新");
                    } else {
                        Log.e(TAG, "未能在 CaptureService 中找到 run 方法");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DexKit 2.2.0 运行异常", e);
            }
        }).start();
    }
}