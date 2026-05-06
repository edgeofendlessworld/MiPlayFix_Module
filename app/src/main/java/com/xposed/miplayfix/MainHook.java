package com.xposed.miplayfix;

import android.content.Context;

import java.io.InputStream;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.milink.service";

    // =========================
    // delay hook
    // =========================
    private static final String DELAY_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";

    private static final String DELAY_METHOD =
            "setAudioPlayDelayTime";

    // =========================
    // audio send hook
    // =========================
    private static final String CAPTURE_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.CaptureService";

    private static final String CAPTURE_METHOD =
            "sendLocalAudio";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log("MiPlayFix: 已注入 -> " + TARGET_PACKAGE);

        // =====================================================
        // ① delay hook
        // =====================================================
        try {
            XposedHelpers.findAndHookMethod(
                    DELAY_CLASS,
                    lpparam.classLoader,
                    DELAY_METHOD,
                    long.class,
                    int.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                            int originalDelay = (int) param.args[1];

                            int newDelay = 50000; // 50ms（微秒）

                            param.args[1] = newDelay;

                            XposedBridge.log(
                                    "MiPlayFix: delay " + originalDelay + " -> " + newDelay
                            );
                        }
                    }
            );

        } catch (Throwable e) {
            XposedBridge.log("delay hook失败 -> " + e);
        }

        // =====================================================
        // ② audio hook（threshold = 50）
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
                            // 替换 2000 -> 50
                            // =========================
                            long threshold = 50;

                            Context context =
                                    (Context) XposedHelpers.getObjectField(thisObj, "mContext");

                            InputStream open = context.getAssets().open(fileName);

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

                                // 推流
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
            XposedBridge.log("sendLocalAudio hook失败 -> " + e);
        }
    }
}