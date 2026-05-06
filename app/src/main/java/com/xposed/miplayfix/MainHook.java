public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.xiaomi.miplay")) {
            return;
        }

        Class<?> clazz = XposedHelpers.findClass(
                "com.xiaomi.miplay.mylibrary.mirror.CaptureService",
                lpparam.classLoader
        );

        XposedHelpers.findAndHookMethod(
                clazz,
                "sendLocalAudio",
                "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl",
                String.class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        // ❗ 阻止原方法执行
                        param.setResult(null);

                        Object thisObj = param.thisObject;

                        Object multiMirrorControl = param.args[0];
                        String fileName = (String) param.args[1];

                        // ===== 可配置变量 =====
                        int threshold = 2000; // 👉 这里就是你要的变量（可改成SP读取）

                        InputStream open = (InputStream) XposedHelpers.callMethod(
                                XposedHelpers.getObjectField(thisObj, "context"),
                                "getAssets"
                        );

                        open = ((android.content.res.AssetManager)
                                XposedHelpers.callMethod(
                                        XposedHelpers.getObjectField(thisObj, "context"),
                                        "getAssets"
                                )).open(fileName);

                        byte[] buffer = new byte[20480];

                        long playSysTime = System.currentTimeMillis();
                        long pts = 0;

                        while (true) {

                            if ((boolean) XposedHelpers.getObjectField(thisObj, "mUseLocalAudioExit")) {
                                break;
                            }

                            if (open.read(buffer, 0, 7) != 7) {
                                break;
                            }

                            int frameSize = (int) XposedHelpers.callMethod(
                                    thisObj,
                                    "parseADTSHeader",
                                    buffer,
                                    7,
                                    0
                            );

                            if (frameSize <= 7 || frameSize > 20480) {
                                break;
                            }

                            int remain = frameSize - 7;

                            if (open.read(buffer, 7, remain) != remain) {
                                break;
                            }

                            byte[] packet = new byte[frameSize];
                            System.arraycopy(buffer, 0, packet, 0, frameSize);

                            // ===== 推流 =====
                            XposedHelpers.callMethod(
                                    multiMirrorControl,
                                    "WriteStream",
                                    false,
                                    packet,
                                    pts
                            );

                            int frameNums = (int) XposedHelpers.getObjectField(thisObj, "frameNums");
                            int samplerate = (int) XposedHelpers.getObjectField(thisObj, "samplerate");

                            pts += (1000000L * frameNums) / samplerate;

                            long currentTimeMillis =
                                    (pts / 1000)
                                            - (System.currentTimeMillis() - playSysTime);

                            // ===== 关键修改点 =====
                            if (currentTimeMillis > threshold) {
                                Thread.sleep(currentTimeMillis - threshold);
                            }
                        }
                    }
                }
        );
    }
}