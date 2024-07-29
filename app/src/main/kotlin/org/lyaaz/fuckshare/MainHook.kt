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
            val callingPackage =
                XposedHelpers.getObjectField(key, "packageName") as? String ?: return
            process(intent, callingPackage, param)
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
        private val actionHookEnableMap = mapOf(
            Intent.ACTION_SEND to { settings.enableForceForwardHook },
            Intent.ACTION_SEND_MULTIPLE to { settings.enableForceForwardHook },
            Intent.ACTION_PICK to { settings.enableForcePickerHook },
            Intent.ACTION_GET_CONTENT to { settings.enableForceContentHook },
            Intent.ACTION_OPEN_DOCUMENT to { settings.enableForceDocumentHook }
        )
        private val actionClassMap = mapOf(
            Intent.ACTION_SEND to HandleShareActivity::class.java.name,
            Intent.ACTION_SEND_MULTIPLE to HandleShareActivity::class.java.name,
            Intent.ACTION_PICK to ContentProxyActivity::class.java.name,
            Intent.ACTION_GET_CONTENT to ContentProxyActivity::class.java.name,
            Intent.ACTION_OPEN_DOCUMENT to ContentProxyActivity::class.java.name
        )

        private fun process(intent: Intent, callingPackage: String, param: MethodHookParam) {
            if (callingPackage in neverHookList || intent.action !in hookedIntents) {
                return
            }

            prefs.reload()
            if (!settings.enableHook) {
                return
            }
            val extraIntent = retrieveExtraIntent(intent) ?: return
            if (!actionHookEnableMap.getOrDefault(extraIntent.action) { false }.invoke()) {
                return
            }
            val className = actionClassMap[extraIntent.action] ?: return

            extraIntent.apply {
                setClassName(BuildConfig.APPLICATION_ID, className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            param.args[3] = extraIntent
            XposedBridge.log("FS: hooked from $callingPackage, intent: $intent, to: $extraIntent")
        }

        private fun retrieveExtraIntent(intent: Intent): Intent? {
            return if (intent.action == Intent.ACTION_CHOOSER) {
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
                } ?: return null
            } else {
                intent.component?.let {
                    if (it.packageName != "com.android.documentsui") {
                        return null
                    }
                }
                intent
            }
        }
    }
}
