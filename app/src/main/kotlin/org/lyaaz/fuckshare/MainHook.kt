package org.lyaaz.fuckshare

import android.content.Intent
import android.os.Build
import android.service.chooser.ChooserAction
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.lyaaz.fuckshare.utils.IntentUtils

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
                "startActivityAsUser",
                StartActivityAsUserHook
            )
            XposedBridge.hookAllMethods(
                activityTaskManagerServiceClass,
                "startActivityIntentSender",
                StartActivityIntentSenderHook
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private object StartActivityAsUserHook : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
            val callingPackage = param.args[1] as? String ?: return
            val intent = param.args[3] as? Intent ?: return
            process(intent, callingPackage, param)
        }
    }

    private object StartActivityIntentSenderHook : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val key = XposedHelpers.getObjectField(param.args[1], "key")
            val intent = XposedHelpers.getObjectField(key, "requestIntent") as? Intent ?: return
            val packageName = XposedHelpers.getObjectField(key, "packageName") as? String ?: return
            process(intent, packageName, param)
        }
    }

    companion object {
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
        private val neverHookList = setOf(
            BuildConfig.APPLICATION_ID,
            "com.android.providers.media.module"
        )

        private fun process(intent: Intent, callingPackage: String, param: MethodHookParam) {
            if (callingPackage in neverHookList || intent.action !in hookedIntents) {
                return
            }

            prefs.reload()
            if (!settings.enableHook) {
                return
            }
            val extraIntent = if (intent.action == Intent.ACTION_CHOOSER) {
                IntentUtils.getParcelableExtra(
                    intent,
                    Intent.EXTRA_INTENT,
                    Intent::class.java
                )?.apply {
                    setOf(Intent.EXTRA_INITIAL_INTENTS, Intent.EXTRA_ALTERNATE_INTENTS).forEach {
                        IntentUtils.backupArrayExtras<Intent>(
                            intent,
                            this,
                            it
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        IntentUtils.backupArrayExtras<ChooserAction>(
                            intent,
                            this,
                            Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
                        )
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
                XposedBridge.log("FS: hooked from $callingPackage, intent: $intent, to: $it")
            }
        }

        private fun handleExtraIntent(extraIntent: Intent): Intent? {
            val className = when (extraIntent.action) {
                Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE ->
                    if (settings.enableForceForwardHook) {
                        HandleShareActivity::class.java.name
                    } else {
                        null
                    }

                Intent.ACTION_PICK ->
                    if (settings.enableForcePickerHook) {
                        ContentProxyActivity::class.java.name
                    } else {
                        null
                    }

                Intent.ACTION_GET_CONTENT ->
                    if (settings.enableForceContentHook) {
                        ContentProxyActivity::class.java.name
                    } else {
                        null
                    }

                Intent.ACTION_OPEN_DOCUMENT ->
                    if (settings.enableForceDocumentHook) {
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
