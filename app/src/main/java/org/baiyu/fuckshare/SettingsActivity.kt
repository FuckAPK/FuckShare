package org.baiyu.fuckshare;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.gyf.immersionbar.ImmersionBar;

public class SettingsActivity extends AppCompatActivity {
    private static SharedPreferences prefs;

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        ImmersionBar.with(this)
                .statusBarView(findViewById(R.id.status_bar_view))
                .transparentStatusBar()
                .transparentNavigationBar()
                .init();
        setStatusBarFontColor(getResources().getConfiguration());
        try {
            prefs = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", MODE_WORLD_READABLE);
        } catch (Exception e) {
            prefs = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", MODE_PRIVATE);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsFragment())
                .commit();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setStatusBarFontColor(newConfig);
    }

    private void setStatusBarFontColor(Configuration conf) {
        boolean darkMode = (conf.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        ImmersionBar.with(this)
                .statusBarDarkFont(!darkMode, 0.2f)
                .init();
    }

    public static class MySettingsFragment extends PreferenceFragmentCompat {
        private static final String PREF_KEEP_LAUNCHER_ICON = "keep_launcher_icon";
        private static final String LAUNCHER_ACTIVITY_NAME = BuildConfig.APPLICATION_ID + ".LauncherActivity";

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            SwitchPreferenceCompat keepLauncherIconPreference = findPreference(PREF_KEEP_LAUNCHER_ICON);
            assert keepLauncherIconPreference != null;

            updateLauncherActivityStatus();
            keepLauncherIconPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                enableDisableLauncherIcon((Boolean) newValue);
                return true;
            });
        }

        private void updateLauncherActivityStatus() {
            Context context = getContext();
            assert context != null;
            PackageManager pm = context.getApplicationContext().getPackageManager();
            ComponentName cn = new ComponentName(BuildConfig.APPLICATION_ID, LAUNCHER_ACTIVITY_NAME);
            boolean status = pm.getComponentEnabledSetting(cn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_KEEP_LAUNCHER_ICON, status);
            editor.apply();
        }

        private void enableDisableLauncherIcon(boolean enable) {
            Context context = getContext();
            assert context != null;
            PackageManager pm = context.getApplicationContext().getPackageManager();
            ComponentName cn = new ComponentName(BuildConfig.APPLICATION_ID, LAUNCHER_ACTIVITY_NAME);
            int status = enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(cn, status, PackageManager.DONT_KILL_APP);
            updateLauncherActivityStatus();
        }
    }

}
