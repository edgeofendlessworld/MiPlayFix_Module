package com.example.miplayfix

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

class MainModule : XposedModule() {

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {

        val packageName = param.packageName
        log("onPackageReady: $packageName")

        if (!param.isFirstPackage) return
        if (packageName != "com.milink.service") return

        try {
            val cl = param.classLoader

            val clazz = cl.loadClass(
                "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl"
            )

            val method: Method = clazz.getDeclaredMethod(
                "setAudioPlayDelayTime",
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )

            xposedModule.hook(method).intercept { chain ->

                val args = chain.args

                val original = args[1] as Int

                val newDelay = 50000
                args[1] = newDelay

                log("delay $original -> $newDelay")

                chain.proceed()
            }

        } catch (t: Throwable) {
            log("hook failed: $t")
        }
    }
}