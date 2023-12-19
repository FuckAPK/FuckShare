package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.content.SharedPreferences
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
        try {
            @Suppress("DEPRECATION")
            prefs = getSharedPreferences(
                BuildConfig.APPLICATION_ID + "_preferences",
                MODE_WORLD_READABLE
            )
        } catch (e: Exception) {
            prefs = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", MODE_PRIVATE)
        }
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
                    Utils.setActivityStatus(
                        requireContext(),
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
                    Utils.setActivityStatus(
                        requireContext(),
                        VIEWER_ACTIVITY_NAME,
                        newValue as Boolean
                    )
                    updateViewerActivityStatus()
                    true
                }
        }

        private fun updateLauncherActivityStatus() {
            val status = Utils.getActivityStatus(requireContext(), LAUNCHER_ACTIVITY_NAME)
            val editor = prefs!!.edit()
            editor.putBoolean(PREF_KEEP_LAUNCHER_ICON, status)
            editor.apply()
        }

        private fun updateViewerActivityStatus() {
            val status = Utils.getActivityStatus(requireContext(), VIEWER_ACTIVITY_NAME)
            val editor = prefs!!.edit()
            editor.putBoolean(PREF_ENABLE_VIEWER, status)
            editor.apply()
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