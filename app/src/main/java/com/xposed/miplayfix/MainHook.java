package com.xposed.miplayfix;

import android.content.Context;

import java.io.InputStream;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.XposedModule;
import io.github.libxposed.api.XposedHelpers;
import io.github.libxposed.api.XposedBridge;
import io.github.libxposed.api.callbacks.MethodHook;

@XposedModule(
        packageName = "com.xposed.miplayfix",
        description = "MiPlayFix (libxposed100)"
)
public class MainHook implements XposedModuleInterface {

    private static final String TARGET_PACKAGE = "com.milink.service";

    private static final String CONTROL_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";

    private static final String CONTROL_METHOD =
            "setAudioPlayDelayTime";

    private static final String CAPTURE_CLASS =
            "com.xiaomi.miplay.mylibrary.mirror.CaptureService";

    private static final String CAPTURE_METHOD =
            "sendLocalAudio";

    @Override
    public void onPackageLoaded(XposedInterface xposed, LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.getPackageName().equals(TARGET_PACKAGE)) return;

        XposedBridge.log("MiPlayFix(libxposed100): injected -> " + TARGET_PACKAGE);

        ClassLoader cl = lpparam.getClassLoader();

        // =========================
        // ① delay hook
        // =========================
        try {
            var method = XposedHelpers.findMethodExact(
                    CONTROL_CLASS,
                    cl,
                    CONTROL_METHOD,
                    long.class,
                    int.class
            );

            XposedBridge.hookMethod(method, new MethodHook() {
                @Override
                public void beforeCall(MethodHookParam param) throws Throwable {

                    int originalDelay = (int) param.getArgs()[1];

                    int newDelay = 50000; // 50ms

                    param.getArgs()[1] = newDelay;

                    XposedBridge.log(
                            "MiPlayFix: delay " + originalDelay + " -> " + newDelay
                    );
                }
            });

        } catch (Throwable e) {
            XposedBridge.log("delay hook failed -> " + e);
        }

        // =========================
        // ② sendLocalAudio hook
        // =========================
        try {
            var method = XposedHelpers.findMethodExact(
                    CAPTURE_CLASS,
                    cl,
                    CAPTURE_METHOD,
                    XposedHelpers.findClass(
                            "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl",
                            cl
                    ),
                    String.class
            );

            XposedBridge.hookMethod(method, new MethodHook() {

                @Override
                public Object replaceCall(MethodHookParam param) throws Throwable {

                    Object thisObj = param.getThisObject();
                    Object multiMirrorControl = param.getArgs()[0];
                    String fileName = (String) param.getArgs()[1];

                    long threshold = 50;

                    Context context = (Context)
                            XposedHelpers.getObjectField(thisObj, "mContext");

                    InputStream open = context.getAssets().open(fileName);

                    byte[] buffer = new byte[20480];

                    long playSysTime = System.currentTimeMillis();
                    long pts = 0;

                    while (true) {

                        boolean exit = (boolean)
                                XposedHelpers.getObjectField(thisObj, "mUseLocalAudioExit");

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

                        XposedHelpers.callMethod(
                                multiMirrorControl,
                                "WriteStream",
                                false,
                                packet,
                                pts
                        );

                        int frameNums = (int)
                                XposedHelpers.getObjectField(thisObj, "frameNums");

                        int samplerate = (int)
                                XposedHelpers.getObjectField(thisObj, "samplerate");

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
            });

        } catch (Throwable e) {
            XposedBridge.log("sendLocalAudio hook failed -> " + e);
        }
    }
}