package com.xposed.miplayfix;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.InputStream;
import java.io.IOException;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // 仅作用于小米妙播进程
        if (!lpparam.packageName.equals("com.xiaomi.miplay")) {
            return;
        }

        // 找到目标类
        Class<?> clazz = XposedHelpers.findClass(
                "com.xiaomi.miplay.mylibrary.mirror.CaptureService",
                lpparam.classLoader
        );

        // Hook 发送音频的方法
        XposedHelpers.findAndHookMethod(
                clazz,
                "sendLocalAudio",
                "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl",
                String.class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // ❗ 阻止原方法执行，改为自定义逻辑
                        param.setResult(null);

                        final Object thisObj = param.thisObject;
                        final Object multiMirrorControl = param.args[0];
                        final String fileName = (String) param.args[1];

                        // 获取 Context 和 AssetManager
                        Context context = (Context) XposedHelpers.getObjectField(thisObj, "context");
                        AssetManager assetManager = context.getAssets();

                        // 开启新线程运行，防止阻塞主线程导致 ANR
                        new Thread(() -> {
                            InputStream is = null;
                            try {
                                is = assetManager.open(fileName);
                                byte[] buffer = new byte[20480];
                                long playSysTime = System.currentTimeMillis();
                                long pts = 0;
                                int threshold = 50; // 缓存阈值（毫秒）

                                while (true) {
                                    // 检查退出标志
                                    if ((boolean) XposedHelpers.getObjectField(thisObj, "mUseLocalAudioExit")) {
                                        break;
                                    }

                                    // 读取 ADTS 头部 (7字节)
                                    if (is.read(buffer, 0, 7) != 7) break;

                                    // 解析帧大小
                                    int frameSize = (int) XposedHelpers.callMethod(
                                            thisObj, "parseADTSHeader", buffer, 7, 0);

                                    if (frameSize <= 7 || frameSize > 20480) break;

                                    // 读取剩余帧数据
                                    int remain = frameSize - 7;
                                    if (is.read(buffer, 7, remain) != remain) break;

                                    // 复制当前帧
                                    byte[] packet = new byte[frameSize];
                                    System.arraycopy(buffer, 0, packet, 0, frameSize);

                                    // 推送到流
                                    XposedHelpers.callMethod(multiMirrorControl, "WriteStream", false, packet, pts);

                                    // 计算下一帧的 PTS
                                    int frameNums = (int) XposedHelpers.getObjectField(thisObj, "frameNums");
                                    int samplerate = (int) XposedHelpers.getObjectField(thisObj, "samplerate");
                                    pts += (1000000L * frameNums) / samplerate;

                                    // 控速逻辑
                                    long sleepTime = (pts / 1000) - (System.currentTimeMillis() - playSysTime);
                                    if (sleepTime > threshold) {
                                        Thread.sleep(sleepTime - threshold);
                                    }
                                }
                            } catch (Exception e) {
                                // 打印错误日志到 LSPosed/Xposed 管理器
                                de.robv.android.xposed.XposedBridge.log("MiPlayFix Error: " + e.getMessage());
                            } finally {
                                if (is != null) {
                                    try { is.close(); } catch (IOException ignored) {}
                                }
                            }
                        }).start();
                    }
                }
        );
    }
}