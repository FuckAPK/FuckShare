package org.baiyu.fuckshare

import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "android") {
            return
        }
        XposedBridge.hookAllMethods(
            XposedHelpers.findClass(
                "com.android.server.wm.ActivityTaskManagerService",
                lpparam.classLoader
            ),
            "startActivity",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val callingPackage = param.args[1] as? String ?: return
                    val intent = param.args[3] as? Intent ?: return

                    if (callingPackage == BuildConfig.APPLICATION_ID || intent.action !in hookedIntent) {
                        return
                    }

                    prefs.reload()
                    if (!settings.enableHook()) {
                        return
                    }

                    val extraIntent = if (intent.action == Intent.ACTION_CHOOSER) {
                        Utils.getParcelableExtra(
                            intent,
                            Intent.EXTRA_INTENT,
                            Intent::class.java
                        )?.apply {
                            Utils.getParcelableArrayExtra<Intent>(
                                intent,
                                Intent.EXTRA_INITIAL_INTENTS
                            )?.let {
                                putExtra(Intent.EXTRA_INITIAL_INTENTS, it)
                            }
                        } ?: return
                    } else {
                        if (intent.component?.packageName == "com.android.documentsui") {
                            return
                        }
                        intent
                    }

                    extraIntent.let {
                        if ((Intent.ACTION_SEND == it.action || Intent.ACTION_SEND_MULTIPLE == it.action)
                            && settings.enableForceForwardHook()
                        ) {
                            param.args[3] = it.apply {
                                setClassName(
                                    BuildConfig.APPLICATION_ID,
                                    HandleShareActivity::class.java.name
                                )
                            }
                        } else if (Intent.ACTION_PICK == it.action && settings.enableForcePickerHook()
                            || Intent.ACTION_GET_CONTENT == it.action && settings.enableForceContentHook()
                            || Intent.ACTION_OPEN_DOCUMENT == it.action && settings.enableForceDocumentHook()
                        ) {
                            param.args[3] = it.apply {
                                setClassName(
                                    BuildConfig.APPLICATION_ID,
                                    ContentProxyActivity::class.java.name
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    companion object {
        private val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID)
        private val settings = Settings.getInstance(prefs)
        private val hookedIntent = setOf(
            Intent.ACTION_CHOOSER,
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE,
            Intent.ACTION_PICK,
            Intent.ACTION_GET_CONTENT,
            Intent.ACTION_OPEN_DOCUMENT
        )
    }
}