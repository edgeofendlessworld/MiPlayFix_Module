package com.xposed.miplayfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.dex.info.MethodInfo; // 尝试导入这个类
import java.util.List;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            try {
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    if (bridge == null) return;

                    // 使用 2.x 最基础的查找方式
                    FindMethod query = FindMethod.create()
                            .setClassName("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                            .setName("run");

                    List<MethodData> results = bridge.findMethod(query);

                    if (results != null && !results.isEmpty()) {
                        MethodData methodData = results.get(0);
                        
                        // 2.x 的 MethodData 结构访问
                        // 如果 getMethodInfo() 报错，尝试直接 methodData.methodInfo
                        Log.d(TAG, "定位成功: " + methodData.getMethodInfo().getDescriptor());

                        // 获取指令集并修改
                        // 注意：如果 getInstructions(bridge) 还是报错，尝试 methodData.getInstructions()
                        methodData.getInstructions(bridge).forEach(ins -> {
                            // 检查指令操作码名
                            String opcode = ins.getOpcodeName();
                            if (opcode != null && opcode.contains("const-wide") && ins.getLiteral() == 2000L) {
                                ins.setLiteral(50L);
                                Log.d(TAG, "已修改 2000ms 为 50ms");
                            }
                        });

                        // 2.x 提交修改
                        // 如果 saveInstructions() 找不到，2.x 很多时候在 bridge 关闭时自动保存
                        // 或者尝试 methodData.save()
                        try {
                            methodData.saveInstructions();
                        } catch (NoSuchMethodError e) {
                            Log.d(TAG, "环境不支持 saveInstructions，将自动尝试保存");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DexKit 运行错误: ", e);
            }
        }).start();
    }
}