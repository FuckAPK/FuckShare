package org.baiyu.fuckshare

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.baiyu.fuckshare.utils.AppUtils
import org.baiyu.fuckshare.ui.AppTheme as Theme

class SettingsActivity : ComponentActivity() {

    private var currentUiMode: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = bottom)
            insets
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
    val focusManager = LocalFocusManager.current
    val prefs = remember { AppUtils.getPrefs(context) }
    val settings = remember { Settings.getInstance(prefs) }
    val isKeyboardOpen by keyboardAsState()

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen) {
            focusManager.clearFocus()
        }
    }

    BackHandler {
        focusManager.clearFocus()
    }

    var enableRemoveExif by remember {
        mutableStateOf(
            settings.enableRemoveExif()
        )
    }
    var enableFallbackToFile by remember {
        mutableStateOf(
            settings.enableFallbackToFile()
        )
    }
    var exifTagsToKeep by remember {
        mutableStateOf(
            settings.exifTagsToKeep.joinToString(", ")
        )
    }

    var enableFileTypeSniff by remember {
        mutableStateOf(
            settings.enableFileTypeSniff()
        )
    }
    var enableArchiveTypeSniff by remember {
        mutableStateOf(
            settings.enableArchiveTypeSniff()
        )
    }
    var enableImageRename by remember {
        mutableStateOf(
            settings.enableImageRename()
        )
    }
    var enableFileRename by remember {
        mutableStateOf(
            settings.enableFileRename()
        )
    }

    var enableHook by remember {
        mutableStateOf(
            settings.enableHook()
        )
    }
    var enableForceForwardHook by remember {
        mutableStateOf(
            settings.enableForceForwardHook()
        )
    }
    var enableForcePickerHook by remember {
        mutableStateOf(
            settings.enableForcePickerHook()
        )
    }
    var enableForceContentHook by remember {
        mutableStateOf(
            settings.enableForceContentHook()
        )
    }
    var enableForceDocumentHook by remember {
        mutableStateOf(
            settings.enableForceDocumentHook()
        )
    }

    var toastTime by remember {
        mutableStateOf(
            settings.toastTime.toString()
        )
    }

    LazyColumn {

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
                    enabled = enableRemoveExif,
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
                        exifTagsToKeep = if (it.endsWith("\n")) {
                            focusManager.clearFocus()
                            it.trim()
                        } else {
                            it
                        }
                        prefs.edit { putString(Settings.PREF_EXIF_TAGS_TO_KEEP, exifTagsToKeep) }
                    },
                    keyboardType = KeyboardType.Ascii
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
                    enabled = enableFileTypeSniff,
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
                    enabled = enableHook,
                    onCheckedChange = {
                        enableForceForwardHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_FORWARD_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_picker_hook,
                    summary = R.string.desc_enable_force_picker_hook,
                    checked = enableForcePickerHook,
                    enabled = enableHook,
                    onCheckedChange = {
                        enableForcePickerHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_PICKER_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_content_hook,
                    summary = R.string.desc_enable_force_content_hook,
                    checked = enableForceContentHook,
                    enabled = enableHook,
                    onCheckedChange = {
                        enableForceContentHook = it
                        prefs.edit { putBoolean(Settings.PREF_ENABLE_FORCE_CONTENT_HOOK, it) }
                    }
                )
                PreferenceItem(
                    title = R.string.title_enable_force_document_hook,
                    summary = R.string.desc_enable_force_document_hook,
                    checked = enableForceDocumentHook,
                    enabled = enableHook,
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
                        val intValue = it.toIntOrNull() ?: return@TextFieldPreference
                        if (intValue < 0) {
                            return@TextFieldPreference
                        }
                        toastTime = it
                        prefs.edit { putInt(Settings.PREF_TOAST_TIME, intValue) }
                    },
                    keyboardType = KeyboardType.Number
                )
            }
        }
    }
}

@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
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
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
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
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(if (enabled) 1.0f else 0.6f)
                )
                if (summary != null) {
                    Text(
                        text = stringResource(id = summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(if (enabled) 1.0f else 0.6f)
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}


@Composable
fun TextFieldPreference(
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(id = title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (summary != null) {
            Text(
                text = stringResource(id = summary),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
        }
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
