package com.xposed.miplayfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.dex.info.MethodInfo;
import org.luckypray.dexkit.dex.Instruction; // 必须导入这个类

import java.util.Collection;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            // 注意：DexKitBridge.create(path) 之后必须使用 try-with-resources
            try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                if (bridge == null) return;

                // 2.x 正确的查找类写法：使用 searchMethod
                Collection<MethodData> results = bridge.findMethod(
                        FindMethod.create()
                                .declaredClass("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                                .name("run")
                );

                if (results != null && !results.isEmpty()) {
                    // 取出第一个结果
                    MethodData methodData = results.iterator().next();
                    
                    // 修正 1：获取 MethodInfo
                    MethodInfo info = methodData.getMethodInfo();
                    Log.d(TAG, "定位成功: " + info.getDescriptor());

                    // 修正 2：指令操作必须通过 bridge 调用的 getInstructions(methodData)
                    // 在 2.x 中，MethodData 不直接包含指令，而是作为 Key 去 bridge 中查
                    java.util.List<Instruction> insns = bridge.getInstructions(methodData);
                    
                    if (insns != null) {
                        for (Instruction ins : insns) {
                            // 修正 3：使用 getOpcodeName() 匹配
                            if (ins.getOpcodeName().contains("const-wide") && ins.getLiteral() == 2000L) {
                                // 修正 4：设置新值
                                ins.setLiteral(50L);
                                Log.d(TAG, "成功修改延迟阈值为 50ms");
                            }
                        }
                        
                        // 修正 5：统一通过 bridge 保存该方法的指令更改
                        bridge.saveInstructions(methodData, insns);
                        Log.d(TAG, "修改已持久化到内存");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DexKit 2.x 执行失败: ", e);
            }
        }).start();
    }
}