package com.example.miplayfix; // 记得确认包名

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

import java.lang.reflect.Method;

// 1. 必须继承 XposedModule
public class MainHook extends XposedModule {

    // 2. 构造函数是必须的，框架会通过反射调用它
    public MainHook(XposedInterface base, XposedModuleInterface module) {
        super(base, module);
    }

    // 3. 重写 onPackageLoaded 来过滤应用包名
    @Override
    public void onPackageLoaded(XposedInterface.PackageLoadedParam param) {
        super.onPackageLoaded(param);
        
        if (!param.getPackageName().equals("com.milink.service")) return;

        log("MiPlayFix: 成功注入目标应用 (LibXposed 模式)");

        try {
            // 通过 ClassLoader 获取目标类
            Class<?> targetClass = param.getClassLoader().loadClass("com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl");
            
            // 使用标准的 Java 反射获取目标方法
            Method targetMethod = targetClass.getDeclaredMethod("setAudioPlayDelayTime", long.class, int.class);

            // 注册 Hook（绑定我们下面定义的 Hooker 类）
            hook(targetMethod, AudioDelayHooker.class);
            
            log("MiPlayFix: AudioDelayHooker 挂载成功！");
            
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log("MiPlayFix: ❌ 找不到目标类或方法 -> " + e.getMessage());
        }
    }

    // 4. 定义静态 Hooker 类，必须实现 XposedInterface.Hooker 并加上 @XposedHooker 注解
    @XposedHooker
    public static class AudioDelayHooker implements XposedInterface.Hooker {
        
        // 使用 @BeforeInvocation 注解代表在方法执行前拦截
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            
            // 通过 callback.getArgs() 获取参数数组
            // arg[0] 是 long, arg[1] 是 int (即延迟时间 100000)
            
            int newDelay = 50000; // 将延迟强制修改为 20ms
            
            // 覆盖原有参数
            callback.getArgs()[1] = newDelay;
        }
    }
}