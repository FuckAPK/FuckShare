package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.gyf.immersionbar.ImmersionBar

class SettingsActivity : AppCompatActivity() {
    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)
        ImmersionBar.with(this)
            .statusBarView(findViewById(R.id.status_bar_view))
            .transparentStatusBar()
            .transparentNavigationBar()
            .init()
        setStatusBarFontColor(resources.configuration)

        prefs = Utils.getPrefs(this)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, MySettingsFragment())
            .commit()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setStatusBarFontColor(newConfig)
    }

    private fun setStatusBarFontColor(conf: Configuration) {
        val darkMode =
            conf.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        ImmersionBar.with(this)
            .statusBarDarkFont(!darkMode, 0.2f)
            .init()
    }

    class MySettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            // setup toast time
            val toastTimePreferences = findPreference<EditTextPreference>(Settings.PREF_TOAST_TIME)
            toastTimePreferences?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
            // setup keep launcher icon
            val keepLauncherIconPreference = findPreference<SwitchPreferenceCompat>(
                PREF_KEEP_LAUNCHER_ICON
            )!!
            updateLauncherActivityStatus()
            keepLauncherIconPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                    setActivityStatus(
                        LAUNCHER_ACTIVITY_NAME,
                        newValue as Boolean
                    )
                    updateLauncherActivityStatus()
                    true
                }
            // setup viewer
            val viewerPreference = findPreference<SwitchPreferenceCompat>(PREF_ENABLE_VIEWER)!!
            updateViewerActivityStatus()
            viewerPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                    setActivityStatus(
                        VIEWER_ACTIVITY_NAME,
                        newValue as Boolean
                    )
                    updateViewerActivityStatus()
                    true
                }
        }

        private fun updateLauncherActivityStatus() {
            val status = getActivityStatus(LAUNCHER_ACTIVITY_NAME)
            val editor = prefs!!.edit()
            editor.putBoolean(PREF_KEEP_LAUNCHER_ICON, status)
            editor.apply()
        }

        private fun updateViewerActivityStatus() {
            val status = getActivityStatus(VIEWER_ACTIVITY_NAME)
            val editor = prefs!!.edit()
            editor.putBoolean(PREF_ENABLE_VIEWER, status)
            editor.apply()
        }

        private fun getActivityStatus(activityName: String): Boolean {
            val pm = requireContext().applicationContext.packageManager
            val cn = ComponentName(BuildConfig.APPLICATION_ID, activityName)
            return pm.getComponentEnabledSetting(cn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        private fun setActivityStatus(activityName: String, enable: Boolean) {
            val pm = requireContext().applicationContext.packageManager
            val cn = ComponentName(BuildConfig.APPLICATION_ID, activityName)
            val status =
                if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(cn, status, PackageManager.DONT_KILL_APP)
        }

        companion object {
            private const val PREF_KEEP_LAUNCHER_ICON = "keep_launcher_icon"
            private const val LAUNCHER_ACTIVITY_NAME =
                BuildConfig.APPLICATION_ID + ".LauncherActivity"
            private const val PREF_ENABLE_VIEWER = "enable_viewer"
            private const val VIEWER_ACTIVITY_NAME =
                BuildConfig.APPLICATION_ID + ".ViewerActivity"
        }
    }

    companion object {
        private var prefs: SharedPreferences? = null
    }
}