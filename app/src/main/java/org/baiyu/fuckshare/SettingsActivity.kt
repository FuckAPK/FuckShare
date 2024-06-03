package org.baiyu.fuckshare

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.baiyu.fuckshare.utils.AppUtils
import org.baiyu.fuckshare.ui.AppTheme as Theme

class SettingsActivity : ComponentActivity() {

    private var currentUiMode: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        setContent {
            Theme {
                SettingsScreen()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newUiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (newUiMode != currentUiMode) {
            recreate()
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppUtils.getPrefs(context) }

    var enableRemoveExif by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_REMOVE_EXIF,
                Settings.DEFAULT_ENABLE_REMOVE_EXIF
            )
        )
    }
    var enableFallbackToFile by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FALLBACK_TO_FILE,
                Settings.DEFAULT_ENABLE_FALLBACK_TO_FILE
            )
        )
    }
    var exifTagsToKeep by remember {
        mutableStateOf(
            prefs.getString(
                Settings.PREF_EXIF_TAGS_TO_KEEP,
                Settings.DEFAULT_EXIF_TAGS_TO_KEEP.joinToString(", ")
            ) ?: ""
        )
    }

    var enableFileTypeSniff by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FILE_TYPE_SNIFF,
                Settings.DEFAULT_ENABLE_FILE_TYPE_SNIFF
            )
        )
    }
    var enableArchiveTypeSniff by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_ARCHIVE_TYPE_SNIFF,
                Settings.DEFAULT_ENABLE_ARCHIVE_TYPE_SNIFF
            )
        )
    }
    var enableImageRename by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_IMAGE_RENAME,
                Settings.DEFAULT_ENABLE_IMAGE_RENAME
            )
        )
    }
    var enableFileRename by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FILE_RENAME,
                Settings.DEFAULT_ENABLE_FILE_RENAME
            )
        )
    }

    var enableHook by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_HOOK,
                Settings.DEFAULT_ENABLE_HOOK
            )
        )
    }
    var enableForceForwardHook by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FORCE_FORWARD_HOOK,
                Settings.DEFAULT_ENABLE_FORCE_FORWARD_HOOK
            )
        )
    }
    var enableForcePickerHook by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FORCE_PICKER_HOOK,
                Settings.DEFAULT_ENABLE_FORCE_PICKER_HOOK
            )
        )
    }
    var enableForceContentHook by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FORCE_CONTENT_HOOK,
                Settings.DEFAULT_ENABLE_FORCE_CONTENT_HOOK
            )
        )
    }
    var enableForceDocumentHook by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_ENABLE_FORCE_DOCUMENT_HOOK,
                Settings.DEFAULT_ENABLE_FORCE_DOCUMENT_HOOK
            )
        )
    }

    var toastTime by remember {
        mutableStateOf(
            prefs.getInt(
                Settings.PREF_TOAST_TIME,
                Settings.DEFAULT_TOAST_TIME
            ).toString()
        )
    }

    LazyColumn(modifier = Modifier.padding(16.dp, 0.dp)) {

        item {
            PreferenceCategory(title = R.string.title_metadata) {
                PreferenceItem(
                    title = R.string.title_enable_remove_exif,
                    summary = R.string.desc_enable_remove_exif,
                    checked = enableRemoveExif,
                    onCheckedChange = {
                        enableRemoveExif = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_REMOVE_EXIF, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_fallback_to_file,
                    summary = R.string.desc_enable_fallback_to_file,
                    checked = enableFallbackToFile,
                    onCheckedChange = {
                        enableFallbackToFile = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FALLBACK_TO_FILE, it) }
                    }
                )
                TextFieldPreference(
                    title = R.string.title_exif_tags_to_keep,
                    summary = R.string.desc_exif_tags_to_keep,
                    value = exifTagsToKeep,
                    onValueChange = {
                        exifTagsToKeep = it
                        prefs.edit { putString(Settings.PREF_EXIF_TAGS_TO_KEEP, it) }
                    }
                )
            }
        }
        item {
            PreferenceCategory(title = R.string.title_rename) {
                PreferenceItem(
                    title = R.string.title_enable_file_type_sniff,
                    summary = R.string.desc_enable_file_type_sniff,
                    checked = enableFileTypeSniff,
                    onCheckedChange = {
                        enableFileTypeSniff = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FILE_TYPE_SNIFF, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_archive_type_sniff,
                    summary = R.string.desc_enable_archive_type_sniff,
                    checked = enableArchiveTypeSniff,
                    onCheckedChange = {
                        enableArchiveTypeSniff = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_ARCHIVE_TYPE_SNIFF, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_image_rename,
                    summary = null,
                    checked = enableImageRename,
                    onCheckedChange = {
                        enableImageRename = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_IMAGE_RENAME, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_file_rename,
                    summary = null,
                    checked = enableFileRename,
                    onCheckedChange = {
                        enableFileRename = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FILE_RENAME, it) }
                    }
                )
            }
        }
        item {
            PreferenceCategory(title = R.string.title_hook) {
                PreferenceItem(
                    title = R.string.title_enable_hook,
                    summary = R.string.desc_enable_hook,
                    checked = enableHook,
                    onCheckedChange = {
                        enableHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_forward_hook,
                    summary = R.string.desc_enable_force_forward_hook,
                    checked = enableForceForwardHook,
                    onCheckedChange = {
                        enableForceForwardHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_FORWARD_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_picker_hook,
                    summary = R.string.desc_enable_force_picker_hook,
                    checked = enableForcePickerHook,
                    onCheckedChange = {
                        enableForcePickerHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_PICKER_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_content_hook,
                    summary = R.string.desc_enable_force_content_hook,
                    checked = enableForceContentHook,
                    onCheckedChange = {
                        enableForceContentHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_CONTENT_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_document_hook,
                    summary = R.string.desc_enable_force_document_hook,
                    checked = enableForceDocumentHook,
                    onCheckedChange = {
                        enableForceDocumentHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_DOCUMENT_HOOK, it) }
                    }
                )
            }
        }
        item {
            PreferenceCategory(title = R.string.title_miscellaneous) {
                TextFieldPreference(
                    title = R.string.title_toast_time,
                    summary = R.string.desc_toast_time,
                    value = toastTime,
                    onValueChange = {
                        toastTime = it
                        prefs.edit { putInt(Settings.PREF_TOAST_TIME, it.toInt()) }
                    },
                    keyboardType = KeyboardType.Number
                )
            }
        }
    }
}

@Composable
fun PreferenceCategory(
    @StringRes title: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(id = title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
fun PreferenceItem(
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = stringResource(id = summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Composable
fun TextFieldPreference(
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (summary != null) {
            Text(
                text = stringResource(id = summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
