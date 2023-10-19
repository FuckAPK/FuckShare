package org.baiyu.fuckshare;

import android.content.Intent;

import androidx.annotation.NonNull;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private final static XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
    private final static Settings settings = Settings.getInstance(prefs);

    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("android")) {
            return;
        }

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader),
                "startActivity",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!(param.args[1] instanceof String callingPackage) || BuildConfig.APPLICATION_ID.equals(callingPackage)) {
                            return;
                        }
                        if (!(param.args[3] instanceof Intent chooserIntent) || !Intent.ACTION_CHOOSER.equals(chooserIntent.getAction())) {
                            return;
                        }
                        prefs.reload();
                        if (!settings.enableForceForwardHook()) {
                            return;
                        }

                        Intent intent = Utils.getParcelableExtra(chooserIntent, Intent.EXTRA_INTENT, Intent.class);
                        assert intent != null;
                        if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                            param.args[3] = intent.setClassName(
                                    BuildConfig.APPLICATION_ID,
                                    HandleShareActivity.class.getName());
                        }
                    }
                }
        );
    }
}
