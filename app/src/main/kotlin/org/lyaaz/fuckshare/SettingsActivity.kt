package org.lyaaz.fuckshare

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.core.net.toUri
import org.lyaaz.fuckshare.utils.AppUtils
import org.lyaaz.ui.*
import timber.log.Timber
import org.lyaaz.ui.theme.AppTheme as Theme

class SettingsActivity : ComponentActivity() {

    private var currentUiMode: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.timberPlantTree(this)
        enableEdgeToEdge()
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
            Timber.i("Night mode changed from $currentUiMode to $newUiMode")
            recreate()
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    Theme {
        SettingsScreen()
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
            Timber.d("clear focus as keyboard closed")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        item {
            MetadataCategory(settings, prefs)
        }
        item {
            RenameCategory(settings, prefs)
        }
        item {
            VideoToGIFCategory(settings, prefs)
        }
        item {
            MiscellaneousCategory(settings, prefs)
        }
        item {
            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(
                    WindowInsets.systemBars
                )
            )
        }
    }
}

@Composable
fun MetadataCategory(settings: Settings, prefs: SharedPreferences) {
    val focusManager = LocalFocusManager.current
    var enableRemoveExif by remember {
        mutableStateOf(
            settings.enableRemoveExif
        )
    }
    var enableFallbackToFile by remember {
        mutableStateOf(
            settings.enableFallbackToFile
        )
    }
    var exifTagsToKeep by remember {
        mutableStateOf(
            settings.exifTagsToKeep.joinToString(", ")
        )
    }
    return PreferenceCategory(title = R.string.title_metadata) {
        SwitchPreferenceItem(
            title = R.string.title_enable_remove_exif,
            summary = R.string.desc_enable_remove_exif,
            checked = enableRemoveExif,
            onCheckedChange = {
                enableRemoveExif = it
                prefs.edit { putBoolean(Settings.PREF_ENABLE_REMOVE_EXIF, it) }
            }
        )
        AnimatedVisibility(
            visible = enableRemoveExif
        ) {
            Column {
                SwitchPreferenceItem(
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
                        if (it.contains('\n')) {
                            focusManager.clearFocus()
                        }
                        // filter ascii chars
                        exifTagsToKeep = it
                            .filter { c -> c in ('a'..'z') + ('A'..'Z') + ('0'..'9') || c in " ,-_" }
                        prefs.edit { putString(Settings.PREF_EXIF_TAGS_TO_KEEP, exifTagsToKeep) }
                    }
                )
            }
        }
    }
}

@Composable
fun RenameCategory(settings: Settings, prefs: SharedPreferences) {
    var enableFileTypeSniff by remember {
        mutableStateOf(
            settings.enableFileTypeSniff
        )
    }
    var enableImageRename by remember {
        mutableStateOf(
            settings.enableImageRename
        )
    }
    var enableVideoRename by remember {
        mutableStateOf(
            settings.enableVideoRename
        )
    }
    var enableFileRename by remember {
        mutableStateOf(
            settings.enableFileRename
        )
    }
    PreferenceCategory(title = R.string.title_rename) {
        SwitchPreferenceItem(
            title = R.string.title_enable_file_type_sniff,
            summary = R.string.desc_enable_file_type_sniff,
            checked = enableFileTypeSniff,
            onCheckedChange = {
                enableFileTypeSniff = it
                prefs.edit { putBoolean(Settings.PREF_ENABLE_FILE_TYPE_SNIFF, it) }
            }
        )
        SwitchPreferenceItem(
            title = R.string.title_enable_image_rename,
            summary = null,
            checked = enableImageRename,
            onCheckedChange = {
                enableImageRename = it
                prefs.edit { putBoolean(Settings.PREF_ENABLE_IMAGE_RENAME, it) }
            }
        )
        SwitchPreferenceItem(
            title = R.string.title_enable_video_rename,
            summary = null,
            checked = enableVideoRename,
            onCheckedChange = {
                enableVideoRename = it
                prefs.edit { putBoolean(Settings.PREF_ENABLE_FILE_RENAME, it) }
            }
        )
        SwitchPreferenceItem(
            title = R.string.title_enable_others_rename,
            summary = null,
            checked = enableFileRename,
            onCheckedChange = {
                enableFileRename = it
                prefs.edit { putBoolean(Settings.PREF_ENABLE_FILE_RENAME, it) }
            }
        )
    }
}

@Composable
fun VideoToGIFCategory(settings: Settings, prefs: SharedPreferences) {
    val focusManager = LocalFocusManager.current
    val qualityResMap = mapOf(
        Settings.VideoToGIFQualityOptions.LOW to R.string.option_low,
        Settings.VideoToGIFQualityOptions.MEDIUM to R.string.option_medium,
        Settings.VideoToGIFQualityOptions.HIGH to R.string.option_high,
        Settings.VideoToGIFQualityOptions.CUSTOM to R.string.option_custom
    )

    var enableVideoToGIF by remember {
        mutableStateOf(
            settings.enableVideoToGIF
        )
    }
    var videoToGIFForceWithAudio by remember {
        mutableStateOf(
            settings.videoToGIFForceWithAudio
        )
    }
    var videoToGIFSizeKB by remember {
        mutableStateOf(
            settings.videoToGifSizeKB.toString()
        )
    }
    var videoToGIFQuality by remember {
        mutableStateOf(
            Settings.VideoToGIFQualityOptions.fromValue(
                settings.videoToGIFQuality
            )
        )
    }
    var videoToGIFCustomOption by remember {
        mutableStateOf(
            settings.videoToGIFCustomOption
        )
    }
    PreferenceCategory(title = R.string.title_video_to_gif) {
        SwitchPreferenceItem(
            title = R.string.title_enable_video_to_gif,
            summary = R.string.desc_enable_video_to_gif,
            checked = enableVideoToGIF,
            onCheckedChange = {
                enableVideoToGIF = it
                prefs.edit { putBoolean(Settings.PREF_ENABLE_VIDEO_TO_GIF, it) }
            }
        )
        AnimatedVisibility(
            visible = enableVideoToGIF
        ) {
            Column {
                SwitchPreferenceItem(
                    title = R.string.title_video_to_gif_force_with_audio,
                    summary = null,
                    checked = videoToGIFForceWithAudio,
                    onCheckedChange = {
                        videoToGIFForceWithAudio = it
                        prefs.edit { putBoolean(Settings.PREF_VIDEO_TO_GIF_FORCE_WITH_AUDIO, it) }
                    }
                )
                TextFieldPreference(
                    title = R.string.title_video_to_gif_size_KB,
                    summary = null,
                    value = videoToGIFSizeKB,
                    unit = R.string.unit_kB,
                    onValueChange = {
                        val intValue =
                            it.ifBlank { "0" }.toIntOrNull() ?: return@TextFieldPreference
                        if (intValue < 0) {
                            return@TextFieldPreference
                        }
                        videoToGIFSizeKB = intValue.toString()
                        prefs.edit { putInt(Settings.PREF_VIDEO_TO_GIF_SIZE_KB, intValue) }
                    },
                    keyboardType = KeyboardType.Number
                )
                DropDownPreference(
                    title = R.string.title_video_to_gif_quality,
                    summary = null,
                    selected = qualityResMap[videoToGIFQuality] ?: R.string.option_low,
                    content = { onItemSelected ->
                        qualityResMap.forEach { (quality, resId) ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = resId)) },
                                onClick = {
                                    videoToGIFQuality = quality
                                    prefs.edit {
                                        putInt(
                                            Settings.PREF_VIDEO_TO_GIF_QUALITY,
                                            quality.value
                                        )
                                    }
                                    onItemSelected()
                                }
                            )
                        }
                    }
                )
                TextFieldPreference(
                    title = R.string.title_video_to_gif_custom_option,
                    summary = R.string.desc_video_to_gif_custom_option,
                    enabled = videoToGIFQuality == Settings.VideoToGIFQualityOptions.CUSTOM,
                    value = videoToGIFCustomOption,
                    onValueChange = {
                        if (it.contains('\n')) {
                            focusManager.clearFocus()
                        }
                        // filter ascii chars
                        videoToGIFCustomOption = it
                            .filter { c -> c != '\n' }
                        prefs.edit {
                            putString(
                                Settings.PREF_VIDEO_TO_GIF_CUSTOM_OPTION,
                                videoToGIFCustomOption
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MiscellaneousCategory(settings: Settings, prefs: SharedPreferences) {
    val context = LocalContext.current
    val launcherActivityName = "${context.packageName}.LauncherActivity"
    val viewerActivityName = "${context.packageName}.ViewerActivity"
    val permissionRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (AppUtils.hasOverlayPermission(context)) {
            prefs.edit { putBoolean(Settings.PREF_ENABLE_TEXT_TO_LINK_ACTION, true) }
            Toast.makeText(context, R.string.toast_permission_granted, Toast.LENGTH_SHORT).show()
            Timber.i("Permission granted")
        }
        Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
        Timber.w("Permission denied")
        (context as ComponentActivity).recreate()
    }
    var toastTimeMS by remember {
        mutableStateOf(
            settings.toastTimeMS.toString()
        )
    }
    var enableQRCodeToTextAction by remember {
        mutableStateOf(
            settings.enableQRCodeToTextAction
        )
    }
    var enableTextToLinkAction by remember {
        mutableStateOf(
            settings.enableTextToLinkAction && AppUtils.hasOverlayPermission(context)
        )
    }
    var enableViewer by remember {
        mutableStateOf(
            AppUtils.getActivityStatus(context, viewerActivityName)
        )
    }
    var enableLauncherIcon by remember {
        mutableStateOf(
            AppUtils.getActivityStatus(context, launcherActivityName)
        )
    }
    PreferenceCategory(title = R.string.title_miscellaneous) {
        TextFieldPreference(
            title = R.string.title_toast_time,
            summary = R.string.desc_toast_time,
            value = toastTimeMS,
            unit = R.string.unit_ms,
            onValueChange = {
                val intValue =
                    it.ifBlank { "0" }.toIntOrNull() ?: return@TextFieldPreference
                if (intValue < 0) {
                    return@TextFieldPreference
                }
                toastTimeMS = intValue.toString()
                prefs.edit { putInt(Settings.PREF_TOAST_TIME_MS, intValue) }
            },
            keyboardType = KeyboardType.Number
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SwitchPreferenceItem(
                title = R.string.title_enable_qr_code_to_text_action,
                checked = enableQRCodeToTextAction,
                onCheckedChange = {
                    enableQRCodeToTextAction = it
                    prefs.edit { putBoolean(Settings.PREF_ENABLE_QR_CODE_TO_TEXT_ACTION, it) }
                }
            )
            SwitchPreferenceItem(
                title = R.string.title_enable_text_to_link_action,
                summary = R.string.desc_enable_text_to_link_action,
                checked = enableTextToLinkAction,
                onCheckedChange = {
                    if (it) {
                        if (AppUtils.hasOverlayPermission(context)) {
                            enableTextToLinkAction = true
                            prefs.edit {
                                putBoolean(
                                    Settings.PREF_ENABLE_TEXT_TO_LINK_ACTION,
                                    true
                                )
                            }
                        } else {
                            permissionRequestLauncher.launch(
                                Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:${context.packageName}".toUri()
                                )
                            )
                        }
                    } else {
                        enableTextToLinkAction = false
                        prefs.edit {
                            putBoolean(
                                Settings.PREF_ENABLE_TEXT_TO_LINK_ACTION,
                                false
                            )
                        }
                    }
                }
            )
        }
        SwitchPreferenceItem(
            title = R.string.title_enable_viewer,
            summary = R.string.desc_enable_viewer,
            checked = enableViewer,
            onCheckedChange = {
                AppUtils.setActivityStatus(context, viewerActivityName, it)
                enableViewer =
                    AppUtils.getActivityStatus(context, viewerActivityName)
            }
        )
        SwitchPreferenceItem(
            title = R.string.title_keep_launcher_icon,
            summary = null,
            checked = enableLauncherIcon,
            onCheckedChange = {
                AppUtils.setActivityStatus(context, launcherActivityName, it)
                enableLauncherIcon =
                    AppUtils.getActivityStatus(context, launcherActivityName)
            }
        )
        SwitchPreferenceItem(
            title = R.string.title_reset_settings,
            summary = null,
            checked = true,
            noSwitch = true,
            onCheckedChange = {
                prefs.edit { clear() }
                Toast.makeText(context, R.string.toast_settings_reset, Toast.LENGTH_SHORT)
                    .show()
                (context as ComponentActivity).recreate()
            }
        )
    }
}
