package org.baiyu.fuckshare;

import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private final static Settings settings = Settings.getInstance(new XSharedPreferences(BuildConfig.APPLICATION_ID));

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("android")) {
            return;
        }

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader),
                "startActivity",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String callingPackage = (String) param.args[1];
                        Intent chooserIntent = (Intent) param.args[3];
                        if (callingPackage.equals(BuildConfig.APPLICATION_ID)) {
                            return;
                        }
                        if (chooserIntent == null || !Intent.ACTION_CHOOSER.equals(chooserIntent.getAction())) {
                            return;
                        }
                        if (!settings.enableForceForwardHook()) {
                            return;
                        }
                        Intent intent;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent = chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
                        } else {
                            intent = chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT);
                        }
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
