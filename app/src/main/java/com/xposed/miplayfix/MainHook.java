package com.xposed.miplayfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.query.FindMethod; // 使用更通用的类
import java.util.List;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            try {
                // 1. 创建 Bridge
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    if (bridge == null) return;

                    // 2. 绕过 MethodQuery，改用更稳健的查找方式
                    List<MethodData> results = bridge.findMethod(
                        FindMethod.create()
                            .declaredClass("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                            .name("run")
                    );

                    if (!results.isEmpty()) {
                        MethodData methodData = results.get(0);
                        
                        // 修正 1: 2.x 的 MethodData 使用 getMethodInfo() 而不是 methodInfo()
                        // 修正 2: 2.x 的指令获取依然需要传入 bridge
                        Log.d(TAG, "定位成功: " + methodData.getMethodInfo().getDescriptor());

                        // 3. 修改指令
                        methodData.getInstructions(bridge).forEach(ins -> {
                            // 使用指令名称匹配
                            if (ins.getOpcodeName().contains("const-wide") && ins.getLiteral() == 2000L) {
                                ins.setLiteral(50L);
                                Log.d(TAG, "成功将阈值从 2000ms 调整为 50ms");
                            }
                        });

                        // 修正 3: 2.x 需要手动调用 save 指令修改
                        methodData.saveInstructions();
                        Log.d(TAG, "字节码修改已保存");
                    } else {
                        Log.e(TAG, "未找到 CaptureService.run 方法");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DexKit 编译异常或运行异常: ", e);
            }
        }).start();
    }
}