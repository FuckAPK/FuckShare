package org.baiyu.fuckshare;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

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
