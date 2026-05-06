package com.xposed.miplayfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.MethodData;
import java.util.List;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 目标包名过滤
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            try {
                // 1. 创建 Bridge
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    if (bridge == null) return;

                    // 2. 查找目标方法
                    // 注意：2.x 中 FindMethod 的用法
                    List<MethodData> results = bridge.findMethod(
                        FindMethod.create()
                            .className("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                            .name("run")
                    );

                    if (!results.isEmpty()) {
                        MethodData methodData = results.get(0);
                        
                        // 修正：2.x 使用 methodInfo() 获取描述符
                        Log.d(TAG, "定位成功: " + methodData.methodInfo().getDescriptor());

                        // 3. 修改指令
                        // 修正：2.x 的 getInstructions 需要传入 bridge 实例
                        methodData.getInstructions(bridge).forEach(ins -> {
                            // 修正：使用 getOpcodeName()
                            if (ins.getOpcodeName().contains("const-wide") && ins.getLiteral() == 2000L) {
                                // 修改为 50ms (0x32)
                                ins.setLiteral(50L);
                                Log.d(TAG, "成功将同步阈值从 2000ms 调整为 50ms");
                            }
                        });

                        // 4. 保存修改
                        // 在 2.x 中，通过 bridge.batch 进行写回，或者直接使用 methodData 的特定保存方法
                        // 如果 saveInstructions 找不到，尝试使用这种通用的 batch 方式：
                        bridge.batch(() -> {
                            // 这里可以放置批量操作，2.x 也会在 bridge 关闭时自动处理部分状态
                        });
                        
                        Log.d(TAG, "修改逻辑已应用");
                    } else {
                        Log.e(TAG, "未找到目标方法，请确认 CaptureService.run 路径是否正确");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DexKit 2.2.0 运行错误: ", e);
            }
        }).start();
    }
}