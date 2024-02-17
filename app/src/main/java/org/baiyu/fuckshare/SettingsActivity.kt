package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.gyf.immersionbar.ImmersionBar
import org.baiyu.fuckshare.utils.AppUtils

class SettingsActivity : AppCompatActivity() {
    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)
        ImmersionBar.with(this)
            .transparentBar()
            .statusBarView(R.id.status_bar_view)
            .fullScreen(false)
            .init()
        setStatusBarFontColor(resources.configuration)

        prefs = AppUtils.getPrefs(this)

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
            preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())

            // Metadata category
            PreferenceCategory(requireContext()).apply {
                title = getString(R.string.title_metadata)
                isIconSpaceReserved = false
            }.also {
                preferenceScreen.addPreference(it)

                // enable remove exif
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_REMOVE_EXIF
                        title = getString(R.string.title_enable_remove_exif)
                        summary = getString(R.string.desc_enable_remove_exif)
                        setDefaultValue(Settings.DEFAULT_ENABLE_REMOVE_EXIF)
                        isIconSpaceReserved = false
                    }
                )

                // enable fallback to file
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FALLBACK_TO_FILE
                        title = getString(R.string.title_enable_fallback_to_file)
                        summary = getString(R.string.desc_enable_fallback_to_file)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FALLBACK_TO_FILE)
                        isIconSpaceReserved = false
                    }
                )

                // exif tags to remove
                it.addPreference(
                    EditTextPreference(requireContext()).apply {
                        key = Settings.PREF_EXIF_TAGS_TO_KEEP
                        title = getString(R.string.title_exif_tags_to_keep)
                        summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                        setDefaultValue(Settings.DEFAULT_EXIF_TAGS_TO_KEEP.joinToString(separator = ", "))
                        isIconSpaceReserved = false
                    }
                )
            }

            // Rename category
            PreferenceCategory(requireContext()).apply {
                title = getString(R.string.title_rename)
                isIconSpaceReserved = false
            }.also {
                preferenceScreen.addPreference(it)

                // sniff extension
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FILE_TYPE_SNIFF
                        title = getString(R.string.title_enable_file_type_sniff)
                        summary = getString(R.string.desc_enable_file_type_sniff)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FILE_TYPE_SNIFF)
                        isIconSpaceReserved = false
                    }
                )

                // sniff archive extension
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_ARCHIVE_TYPE_SNIFF
                        title = getString(R.string.title_enable_archive_type_sniff)
                        summary = getString(R.string.desc_enable_archive_type_sniff)
                        setDefaultValue(Settings.DEFAULT_ENABLE_ARCHIVE_TYPE_SNIFF)
                        isIconSpaceReserved = false
                    }
                )

                // auto rename images
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_IMAGE_RENAME
                        title = getString(R.string.title_enable_image_rename)
                        setDefaultValue(Settings.DEFAULT_ENABLE_IMAGE_RENAME)
                        isIconSpaceReserved = false
                    }
                )

                // auto rename other files
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FILE_RENAME
                        title = getString(R.string.title_enable_file_rename)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FILE_RENAME)
                        isIconSpaceReserved = false
                    }
                )

                it.findPreference<SwitchPreferenceCompat>(Settings.PREF_ENABLE_ARCHIVE_TYPE_SNIFF)?.dependency =
                    Settings.PREF_ENABLE_FILE_TYPE_SNIFF
            }

            // hook category
            PreferenceCategory(requireContext()).apply {
                title = getString(R.string.title_hook)
                isIconSpaceReserved = false
            }.also {
                preferenceScreen.addPreference(it)

                // enable hook
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_HOOK
                        title = getString(R.string.title_enable_hook)
                        summary = getString(R.string.desc_enable_hook)
                        setDefaultValue(Settings.DEFAULT_ENABLE_HOOK)
                        isIconSpaceReserved = false
                    }
                )

                val otherPreference = mutableListOf<SwitchPreferenceCompat>()
                // forward
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FORCE_FORWARD_HOOK
                        title = getString(R.string.title_enable_force_forward_hook)
                        summary = getString(R.string.desc_enable_force_forward_hook)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FORCE_FORWARD_HOOK)
                        isIconSpaceReserved = false
                    }.also { sp -> otherPreference.add(sp) }
                )

                // pick
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FORCE_PICKER_HOOK
                        title = getString(R.string.title_enable_force_picker_hook)
                        summary = getString(R.string.desc_enable_force_picker_hook)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FORCE_PICKER_HOOK)
                        isIconSpaceReserved = false
                    }.also { sp -> otherPreference.add(sp) }
                )

                // get_content
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FORCE_CONTENT_HOOK
                        title = getString(R.string.title_enable_force_content_hook)
                        summary = getString(R.string.desc_enable_force_content_hook)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FORCE_CONTENT_HOOK)
                        isIconSpaceReserved = false
                    }.also { sp -> otherPreference.add(sp) }
                )

                // open_document
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        key = Settings.PREF_ENABLE_FORCE_DOCUMENT_HOOK
                        title = getString(R.string.title_enable_force_document_hook)
                        summary = getString(R.string.desc_enable_force_document_hook)
                        setDefaultValue(Settings.DEFAULT_ENABLE_FORCE_DOCUMENT_HOOK)
                        isIconSpaceReserved = false
                    }.also { sp -> otherPreference.add(sp) }
                )

                otherPreference.forEach { sp -> sp.dependency = Settings.PREF_ENABLE_HOOK }
            }

            // others category
            PreferenceCategory(requireContext()).apply {
                title = getString(R.string.title_others)
                isIconSpaceReserved = false
            }.also {
                preferenceScreen.addPreference(it)

                // toast time
                it.addPreference(
                    EditTextPreference(requireContext()).apply {
                        key = Settings.PREF_TOAST_TIME
                        title = getString(R.string.title_toast_time)
                        summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

                        setDefaultValue(Settings.DEFAULT_TOAST_TIME.toString())
                        isIconSpaceReserved = false

                        setOnBindEditTextListener { editText ->
                            editText.inputType = InputType.TYPE_CLASS_NUMBER
                        }
                    }
                )

                // viewer
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        title = getString(R.string.title_enable_viewer)
                        summary = getString(R.string.desc_enable_viewer)
                        isIconSpaceReserved = false

                        isChecked = AppUtils.getActivityStatus(
                            requireContext(),
                            VIEWER_ACTIVITY_NAME
                        )

                        onPreferenceChangeListener =
                            Preference.OnPreferenceChangeListener { _, newValue: Any ->
                                AppUtils.setActivityStatus(
                                    requireContext(),
                                    VIEWER_ACTIVITY_NAME,
                                    newValue as Boolean
                                )
                                isChecked = AppUtils.getActivityStatus(
                                    requireContext(),
                                    VIEWER_ACTIVITY_NAME
                                )
                                true
                            }
                    }
                )

                // launcher icon
                it.addPreference(
                    SwitchPreferenceCompat(requireContext()).apply {
                        title = getString(R.string.title_keep_launcher_icon)
                        isIconSpaceReserved = false

                        isChecked = AppUtils.getActivityStatus(
                            requireContext(),
                            LAUNCHER_ACTIVITY_NAME
                        )

                        onPreferenceChangeListener =
                            Preference.OnPreferenceChangeListener { _, newValue: Any ->
                                AppUtils.setActivityStatus(
                                    requireContext(),
                                    LAUNCHER_ACTIVITY_NAME,
                                    newValue as Boolean
                                )
                                isChecked = AppUtils.getActivityStatus(
                                    requireContext(),
                                    LAUNCHER_ACTIVITY_NAME
                                )
                                true
                            }
                    }
                )
            }
        }

        companion object {
            private const val LAUNCHER_ACTIVITY_NAME =
                BuildConfig.APPLICATION_ID + ".LauncherActivity"
            private const val VIEWER_ACTIVITY_NAME =
                BuildConfig.APPLICATION_ID + ".ViewerActivity"
        }
    }

    companion object {
        private lateinit var prefs: SharedPreferences
    }
}
