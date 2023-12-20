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

        try {
            val activityTaskManagerServiceClass = XposedHelpers.findClass(
                "com.android.server.wm.ActivityTaskManagerService",
                lpparam.classLoader
            )
            XposedBridge.hookAllMethods(
                activityTaskManagerServiceClass,
                "startActivity",
                StartActivityHook
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private object StartActivityHook : XC_MethodHook() {
        private val prefs: XSharedPreferences by lazy {
            XSharedPreferences(BuildConfig.APPLICATION_ID)
        }
        private val settings: Settings by lazy {
            Settings.getInstance(prefs)
        }
        private val hookedIntents = setOf(
            Intent.ACTION_CHOOSER,
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE,
            Intent.ACTION_PICK,
            Intent.ACTION_GET_CONTENT,
            Intent.ACTION_OPEN_DOCUMENT
        )

        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
            val callingPackage = param.args[1] as? String ?: return
            val intent = param.args[3] as? Intent ?: return

            if (callingPackage == BuildConfig.APPLICATION_ID || intent.action !in hookedIntents) {
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
                intent.component?.let {
                    if (it.packageName != "com.android.documentsui") {
                        return
                    }
                }
                intent
            }
            handleExtraIntent(extraIntent)?.let {
                param.args[3] = it
            }
        }

        private fun handleExtraIntent(extraIntent: Intent): Intent? {
            val className = when (extraIntent.action) {
                Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE ->
                    if (settings.enableForceForwardHook()) {
                        HandleShareActivity::class.java.name
                    } else {
                        null
                    }

                Intent.ACTION_PICK ->
                    if (settings.enableForcePickerHook()) {
                        ContentProxyActivity::class.java.name
                    } else {
                        null
                    }

                Intent.ACTION_GET_CONTENT ->
                    if (settings.enableForceContentHook()) {
                        ContentProxyActivity::class.java.name
                    } else {
                        null
                    }

                Intent.ACTION_OPEN_DOCUMENT ->
                    if (settings.enableForceDocumentHook()) {
                        ContentProxyActivity::class.java.name
                    } else {
                        null
                    }

                else -> null
            }

            return className?.let {
                extraIntent.setClassName(
                    BuildConfig.APPLICATION_ID,
                    it
                )
            }
        }
    }
}
