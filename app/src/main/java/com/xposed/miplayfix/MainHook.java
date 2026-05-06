package com.xposed.miplayfix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.InputStream;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.milink.service";

    // =========================
    // ✔ 原功能：延迟控制
    // =========================
    private static final String CONTROL_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";

    private static final String CONTROL_METHOD =
            "setAudioPlayDelayTime";

    // =========================
    // ✔ 新增：音频发送控制
    // =========================
    private static final String CAPTURE_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.CaptureService";

    private static final String CAPTURE_METHOD =
            "sendLocalAudio";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log("MiPlayFix: 已注入 -> " + TARGET_PACKAGE);

        // =====================================================
        // ✔ ① 原逻辑：delay time hook（完全不动）
        // =====================================================
        try {
            XposedHelpers.findAndHookMethod(
                    CONTROL_CLASS,
                    lpparam.classLoader,
                    CONTROL_METHOD,
                    long.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            int originalDelay = (int) param.args[1];

                            int newDelay = 50000; // 50ms（微秒）

                            param.args[1] = newDelay;

                            XposedBridge.log(
                                    "MiPlayFix: delay修改 " +
                                    originalDelay + " -> " + newDelay
                            );
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log("MiPlayFix: delay hook失败 -> " + e);
        }

        // =====================================================
        // ✔ ② 新增：sendLocalAudio（只改 2000）
        // =====================================================
        try {
            XposedHelpers.findAndHookMethod(
                    CAPTURE_CLASS,
                    lpparam.classLoader,
                    CAPTURE_METHOD,
                    "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl",
                    String.class,
                    new XC_MethodHook() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                            Object thisObj = param.thisObject;
                            Object multiMirrorControl = param.args[0];
                            String fileName = (String) param.args[1];

                            // =========================
                            // ✔ 唯一可调变量（替换 2000）
                            // =========================
                            long threshold = 50;

                            android.content.Context context =
                                    (android.content.Context)
                                            XposedHelpers.getObjectField(thisObj, "mContext");

                            InputStream open =
                                    context.getAssets().open(fileName);

                            byte[] buffer = new byte[20480];

                            long playSysTime = System.currentTimeMillis();
                            long pts = 0;

                            while (true) {

                                boolean exit =
                                        (boolean) XposedHelpers.getObjectField(
                                                thisObj,
                                                "mUseLocalAudioExit"
                                        );

                                if (exit) break;

                                if (open.read(buffer, 0, 7) != 7) break;

                                int frameSize = (int) XposedHelpers.callMethod(
                                        thisObj,
                                        "parseADTSHeader",
                                        buffer,
                                        7,
                                        0
                                );

                                if (frameSize <= 7 || frameSize > 20480) break;

                                int remain = frameSize - 7;

                                if (open.read(buffer, 7, remain) != remain) break;

                                byte[] packet = new byte[frameSize];
                                System.arraycopy(buffer, 0, packet, 0, frameSize);

                                // =========================
                                // ✔ 推流
                                // =========================
                                XposedHelpers.callMethod(
                                        multiMirrorControl,
                                        "WriteStream",
                                        false,
                                        packet,
                                        pts
                                );

                                int frameNums =
                                        (int) XposedHelpers.getObjectField(thisObj, "frameNums");

                                int samplerate =
                                        (int) XposedHelpers.getObjectField(thisObj, "samplerate");

                                pts += (1000000L * frameNums) / samplerate;

                                long currentTimeMillis =
                                        (pts / 1000)
                                                - (System.currentTimeMillis() - playSysTime);

                                // =========================
                                // ✔ 只修改这一处（2000 → threshold）
                                // =========================
                                if (currentTimeMillis > threshold) {
                                    Thread.sleep(currentTimeMillis - threshold);
                                }
                            }

                            return null;
                        }
                    }
            );

            XposedBridge.log("MiPlayFix: sendLocalAudio hook成功");

        } catch (Throwable e) {
            XposedBridge.log("MiPlayFix: sendLocalAudio hook失败 -> " + e);
        }
    }
}