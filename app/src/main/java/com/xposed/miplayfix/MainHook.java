package com.xposed.miplayfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.match.MethodQuery; // 注意包名变了
import org.luckypray.dexkit.result.MethodData;
import java.util.List;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 目标包名确认
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            try {
                // DexKit 2.x 依然使用 create 映射 APK
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    if (bridge == null) return;

                    // 1. 2.x 使用 MethodQuery.create() 配合 className
                    List<MethodData> results = bridge.findMethod(
                        MethodQuery.create()
                            .className("com.xiaomi.miplay.mylibrary.mirror.CaptureService")
                            .name("run")
                    );

                    if (!results.isEmpty()) {
                        MethodData methodData = results.get(0);
                        // 2.x 中使用 methodInfo() 获取方法详情
                        Log.d(TAG, "定位成功: " + methodData.methodInfo().getDescriptor());

                        // 2. 修改指令：2.x 需要传入 bridge 实例
                        methodData.getInstructions(bridge).forEach(ins -> {
                            // 使用 getOpcodeName() 获取指令文本
                            if (ins.getOpcodeName().contains("const-wide") && ins.getLiteral() == 2000L) {
                                // 3. 修改为 50ms
                                ins.setLiteral(50L);
                                Log.d(TAG, "已将 0x7d0 修改为 50ms");
                            }
                        });

                        // 3. 2.x 的修改是实时映射到内存镜像的，只要 bridge 被正确关闭，修改就会写回
                    } else {
                        Log.e(TAG, "未找到目标方法，请确认方法名是否为 run");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DexKit 运行错误: ", e);
            }
        }).start();
    }
}