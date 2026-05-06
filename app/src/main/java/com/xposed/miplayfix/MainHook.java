package com.xposed.miplayfix

import android.content.ComponentName
import android.content.Context
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.util.concurrent.CopyOnWriteArrayList

/**
 * MiPlayFix - 小米投屏服务音频延迟修复模块
 * 基于 libxposed API 101.0.0
 */
class MainModule : XposedModule() {

    companion object {
        private const val TARGET_PACKAGE = "com.milink.service"
        private const val TARGET_CLASS = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl"
        private const val TARGET_METHOD = "setAudioPlayDelayTime"
        private const val NEW_DELAY = 50000 // 微秒，即50ms
        private const val TAG = "MiPlayFix"
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        log("模块已加载")
    }

    override fun onPackageEvent(param: XposedModuleInterface.PackageEvent) {
        // 仅处理目标包
        if (param.packageName != TARGET_PACKAGE) return

        log("成功注入目标应用 -> $TARGET_PACKAGE")

        try {
            hookAudioDelayMethod(param)
        } catch (e: Throwable) {
            log("注入失败 -> ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Hook 音频延迟方法
     */
    private fun hookAudioDelayMethod(param: XposedModuleInterface.PackageEvent) {
        try {
            val classLoader = param.classLoader
            val targetClass = Class.forName(TARGET_CLASS, false, classLoader)
            
            val targetMethod = targetClass.getDeclaredMethod(
                TARGET_METHOD,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )

            hook(targetMethod).before { hookParam ->
                try {
                    val originalDelay = hookParam.args[1] as Int
                    hookParam.args[1] = NEW_DELAY
                    log("音频延迟已修改 [ $originalDelay -> $NEW_DELAY ]")
                } catch (e: Exception) {
                    log("参数修改失败: ${e.message}")
                }
            }

        } catch (e: ClassNotFoundException) {
            log("未找到目标类: $TARGET_CLASS")
        } catch (e: NoSuchMethodException) {
            log("未找到目标方法: $TARGET_METHOD")
        }
    }

    private fun log(message: String) {
        log(android.util.Log.INFO, TAG, message)
    }
}