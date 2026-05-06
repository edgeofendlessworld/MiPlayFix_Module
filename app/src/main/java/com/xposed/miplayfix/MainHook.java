package com.xposed.miplayfix;

import android.util.Log;

import java.io.*;
import java.lang.reflect.*;
import java.util.List;
import java.util.zip.ZipFile;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.writer.builder.*;
import org.jf.dexlib2.writer.io.FileDataStore;

import dalvik.system.BaseDexClassLoader;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "MiPlayFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (!lpparam.packageName.equals("com.xiaomi.miplay")) return;

        new Thread(() -> {
            try {

                Log.d(TAG, "start patch");

                String apkPath = lpparam.appInfo.sourceDir;

                File dex = extractDex(apkPath);
                File patched = patchDex(dex);

                injectDex(lpparam.classLoader, patched);

                Log.d(TAG, "DONE");

            } catch (Throwable e) {
                Log.e(TAG, "error", e);
            }
        }).start();
    }

    // =========================
    // 1️⃣ 提取 dex
    // =========================
    private File extractDex(String apkPath) throws Exception {

        File out = new File("/data/local/tmp/base.dex");

        ZipFile zip = new ZipFile(apkPath);
        InputStream is = zip.getInputStream(zip.getEntry("classes.dex"));

        FileOutputStream fos = new FileOutputStream(out);

        byte[] buf = new byte[4096];
        int len;

        while ((len = is.read(buf)) > 0) {
            fos.write(buf, 0, len);
        }

        fos.close();
        is.close();
        zip.close();

        return out;
    }

    // =========================
    // 2️⃣ dex patch（2000 → 50）
    // =========================
    private File patchDex(File inputDex) throws Exception {

        DexBackedDexFile dexFile =
                DexFileFactory.loadDexFile(inputDex, Opcodes.getDefault());

        DexBuilder builder = new DexBuilder(Opcodes.getDefault());

        for (ClassDef c : dexFile.getClasses()) {

            BuilderClassDef bc = new BuilderClassDef(
                    c.getType(),
                    c.getAccessFlags(),
                    c.getSuperclass(),
                    c.getInterfaces(),
                    c.getSourceFile(),
                    null,
                    null
            );

            for (Method m : c.getMethods()) {

                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;

                MutableMethodImplementation mm =
                        new MutableMethodImplementation(impl);

                List<BuilderInstruction> insns = mm.getInstructions();

                for (int i = 0; i < insns.size(); i++) {

                    BuilderInstruction ins = insns.get(i);

                    if (ins instanceof BuilderInstruction51l) {

                        BuilderInstruction51l i51 =
                                (BuilderInstruction51l) ins;

                        if (i51.getWideLiteral() == 2000L) {

                            Log.d(TAG, "FOUND 2000 -> 50");

                            BuilderInstruction51l newIns =
                                    new BuilderInstruction51l(
                                            i51.getOpcode(),
                                            i51.getRegisterA(),
                                            50L
                                    );

                            mm.replaceInstruction(i, newIns);
                        }
                    }
                }

                bc.getMethods().add(
                        new BuilderMethod(
                                bc,
                                m.getName(),
                                m.getParameters(),
                                m.getReturnType(),
                                m.getAccessFlags(),
                                m.getAnnotations(),
                                m.getHiddenApiRestrictions(),
                                mm
                        )
                );
            }

            builder.internClassDef(bc);
        }

        File out = new File("/data/local/tmp/patched.dex");
        builder.writeTo(new FileDataStore(out));

        return out;
    }

    // =========================
    // 3️⃣ ⭐核心：扩展 dex（生效关键）
    // =========================
    private void injectDex(ClassLoader cl, File dexFile) throws Exception {

        BaseDexClassLoader base = (BaseDexClassLoader) cl;

        Field pathListField = BaseDexClassLoader.class
                .getDeclaredField("pathList");
        pathListField.setAccessible(true);

        Object pathList = pathListField.get(base);

        Field dexElementsField = pathList.getClass()
                .getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);

        Object[] old = (Object[]) dexElementsField.get(pathList);

        Method makeDexElements = pathList.getClass().getDeclaredMethod(
                "makeDexElements",
                List.class,
                File.class,
                List.class,
                ClassLoader.class
        );
        makeDexElements.setAccessible(true);

        List<File> list = new java.util.ArrayList<>();
        list.add(dexFile);

        List<IOException> suppressed = new java.util.ArrayList<>();

        Object[] newElements = (Object[]) makeDexElements.invoke(
                pathList,
                list,
                null,
                suppressed,
                cl
        );

        Object[] combined = (Object[]) Array.newInstance(
                old.getClass().getComponentType(),
                old.length + newElements.length
        );

        System.arraycopy(old, 0, combined, 0, old.length);
        System.arraycopy(newElements, 0, combined, old.length, newElements.length);

        dexElementsField.set(pathList, combined);

        Log.d(TAG, "dex injected -> effective");
    }
}