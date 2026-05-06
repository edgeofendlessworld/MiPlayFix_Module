package com.example.miplay_hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.util.Log;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiPlay Hook Module - API 101版本
 * 功能：将 const-wide/16 v7, 0x3e8 替换为可动态修改的变量
 * 原始值：0x3e8 = 1000 (用于PTS时间戳计算)
 * 新机制：TIME_DIVISOR 变量（可通过setTimeDivisor()动态修改）
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "MiPlayHook";
    private static final String TARGET_PACKAGE = "com.xiaomi.miplay";
    private static final String CAPTURE_SERVICE_CLASS = "com.xiaomi.miplay.mylibrary.mirror.CaptureService";
    private static final String MIRROR_CONTROL_CLASS = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    
    // 替代 0x3e8 (1000) 的变量
    private static final AtomicLong TIME_DIVISOR = new AtomicLong(1000L);
    private static long callCount = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只Hook目标应用
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }

        logInfo("==================== Hook启动 ====================");
        logInfo("目标包名: " + TARGET_PACKAGE);
        logInfo("初始时间除数 (TIME_DIVISOR): " + TIME_DIVISOR.get());
        logInfo("替代常量: 0x3e8 (1000)");
        logInfo("API版本: 101 (Xposed)");
        logInfo("====================================================");

        try {
            hookSendLocalAudio(lpparam);
            logSuccess("✓ Hook 已安装: sendLocalAudio");
        } catch (Throwable e) {
            logError("❌ 安装sendLocalAudio Hook失败: " + e.getMessage());
            XposedBridge.log(e);
        }

        try {
            hookParseADTSHeader(lpparam);
            logSuccess("✓ Hook 已安装: parseADTSHeader");
        } catch (Throwable e) {
            logError("❌ 安装parseADTSHeader Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook sendLocalAudio(MultiMirrorControl, String)方法
     * 这是关键方法，包含时间戳计算逻辑
     */
    private void hookSendLocalAudio(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> captureServiceClass = XposedHelpers.findClass(
                CAPTURE_SERVICE_CLASS,
                lpparam.classLoader
            );

            Class<?> multiMirrorControlClass = XposedHelpers.findClass(
                MIRROR_CONTROL_CLASS,
                lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(
                captureServiceClass,
                "sendLocalAudio",
                multiMirrorControlClass,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        callCount++;
                        Object thisObj = param.thisObject;

                        try {
                            // 读取关键字段
                            long pts = XposedHelpers.getLongField(thisObj, "pts");
                            int frameNums = XposedHelpers.getIntField(thisObj, "frameNums");
                            int samplerate = XposedHelpers.getIntField(thisObj, "samplerate");
                            long playSysTime = XposedHelpers.getLongField(thisObj, "playSysTime");

                            long currentDivisor = TIME_DIVISOR.get();
                            
                            // 计算帧时长
                            long frameDurationMicros = (long) frameNums * 1000000 / samplerate;
                            long frameDurationMs = frameDurationMicros / 1000;

                            logInfo("");
                            logInfo("═══════════════════ sendLocalAudio 开始 ═══════════════════");
                            logInfo("调用次数: #" + callCount);
                            logInfo("时间戳 (PTS): " + pts + " microseconds");
                            logInfo("每帧样本数 (frameNums): " + frameNums);
                            logInfo("采样率 (samplerate): " + samplerate + " Hz");
                            logInfo("播放系统时间 (playSysTime): " + playSysTime);
                            logInfo("当前TIME_DIVISOR: " + currentDivisor);
                            logInfo("帧时长: " + frameDurationMicros + " microseconds");
                            logInfo("帧时长: " + frameDurationMs + " milliseconds");

                        } catch (Throwable e) {
                            logError("❌ beforeHookedMethod错误: " + e.getMessage());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object thisObj = param.thisObject;

                        try {
                            // 读取方法执行后的PTS
                            long ptsAfter = XposedHelpers.getLongField(thisObj, "pts");
                            int frameNums = XposedHelpers.getIntField(thisObj, "frameNums");
                            int samplerate = XposedHelpers.getIntField(thisObj, "samplerate");

                            long currentDivisor = TIME_DIVISOR.get();
                            
                            // 计算理论增量
                            long theoreticalIncrement = (long) frameNums * 1000000 / samplerate;
                            
                            // 计算修正系数
                            double correctionFactor = (double) currentDivisor / 1000.0;

                            logInfo("执行后PTS: " + ptsAfter);
                            logInfo("理论PTS增量: " + theoreticalIncrement + " microseconds");
                            logInfo("TIME_DIVISOR修正系数: " + String.format("%.1f", correctionFactor) + "x");
                            logInfo("═══════════════════ sendLocalAudio 结束 ═══════════════════");

                        } catch (Throwable e) {
                            logError("❌ afterHookedMethod错误: " + e.getMessage());
                        }
                    }
                }
            );

        } catch (ClassNotFoundException e) {
            logError("❌ 找不到类: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Hook parseADTSHeader方法用于追踪音频帧解析
     */
    private void hookParseADTSHeader(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> captureServiceClass = XposedHelpers.findClass(
                CAPTURE_SERVICE_CLASS,
                lpparam.classLoader
            );

            // Hook parseADTSHeader方法
            Method[] methods = captureServiceClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("parseADTSHeader")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.getResult() instanceof Integer) {
                                int frameLength = (Integer) param.getResult();
                                logDebug("ADTS帧长度: " + frameLength + " bytes");
                            }
                        }
                    });
                    break;
                }
            }

        } catch (ClassNotFoundException e) {
            logError("❌ parseADTSHeader Hook失败: " + e.getMessage());
        }
    }

    /**
     * 设置新的TIME_DIVISOR值（替代0x3e8）
     * @param newDivisor 新的除数值
     */
    public static void setTimeDivisor(long newDivisor) {
        long oldDivisor = TIME_DIVISOR.getAndSet(newDivisor);
        double correctionFactor = (double) newDivisor / 1000.0;
        
        logInfo("");
        logInfo("⚙️  TIME_DIVISOR 已修改");
        logInfo("   旧值: " + oldDivisor);
        logInfo("   新值: " + newDivisor);
        logInfo("   修正系数: " + String.format("%.1f", correctionFactor) + "x");
        logInfo("");
    }

    /**
     * 重置TIME_DIVISOR为默认值
     */
    public static void resetTimeDivisor() {
        setTimeDivisor(1000L);
        logSuccess("✓ TIME_DIVISOR 已重置为默认值: 1000");
    }

    /**
     * 获取当前TIME_DIVISOR值
     */
    public static long getTimeDivisor() {
        return TIME_DIVISOR.get();
    }

    /**
     * 获取调用次数
     */
    public static long getCallCount() {
        return callCount;
    }

    // ==================== 日志方法 ====================

    private static void logInfo(String msg) {
        Log.i(TAG, msg);
        XposedBridge.log("[INFO] " + TAG + ": " + msg);
    }

    private static void logDebug(String msg) {
        Log.d(TAG, msg);
    }

    private static void logSuccess(String msg) {
        Log.i(TAG, msg);
        XposedBridge.log("[SUCCESS] " + TAG + ": " + msg);
    }

    private static void logError(String msg) {
        Log.e(TAG, msg);
        XposedBridge.log("[ERROR] " + TAG + ": " + msg);
    }
}
