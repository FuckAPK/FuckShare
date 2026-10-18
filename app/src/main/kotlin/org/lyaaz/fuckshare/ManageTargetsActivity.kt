package org.lyaaz.fuckshare

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lyaaz.fuckshare.utils.AppUtils
import org.lyaaz.ui.PreferenceGroup
import org.lyaaz.ui.SettingsList
import org.lyaaz.ui.SettingsScaffold
import org.lyaaz.ui.SwitchPreferenceItem
import org.lyaaz.ui.theme.AppTheme

data class ShareTargetActivityInfo(
    val componentName: ComponentName,
    val activityName: String,
    val className: String,
    val icon: android.graphics.drawable.Drawable,
    val isEnabled: Boolean,
    val acceptedTypes: Set<String>
)

data class ShareTargetAppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable,
    val isEnabled: Boolean,
    val activities: List<ShareTargetActivityInfo>
)

class ManageTargetsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.timberPlantTree(this)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                ManageTargetsScreen()
            }
        }
    }
}

@Composable
fun ManageTargetsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppUtils.getPrefs(context) }
    var excludedTargets by remember {
        mutableStateOf(prefs.getStringSet(Settings.PREF_EXCLUDED_TARGETS, emptySet()) ?: emptySet())
    }
    var showDisabledApps by remember {
        mutableStateOf(
            prefs.getBoolean(
                Settings.PREF_SHOW_DISABLED_APPS,
                Settings.DEFAULT_SHOW_DISABLED_APPS
            )
        )
    }

    var appTargets by remember { mutableStateOf<List<ShareTargetAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val visibleAppTargets = remember(appTargets, showDisabledApps) {
        if (showDisabledApps) appTargets else appTargets.filter { it.isEnabled }
    }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    var actionsExpanded by remember { mutableStateOf(false) }
    // Rule validity always uses the full scan, never the display-filtered list.
    val activeComponents = remember(appTargets) {
        buildSet {
            appTargets.forEach { app ->
                app.activities.forEach { add(it.componentName.flattenToString()) }
            }
        }
    }
    val inactiveCount = remember(excludedTargets, activeComponents) {
        excludedTargets.count { it !in activeComponents }
    }

    LaunchedEffect(Unit) {
        val loadedTargets = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val types =
                listOf("*/*", "text/plain", "image/*", "video/*", "audio/*", "application/*")
            val actions = listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)

            val activityMap = mutableMapOf<ComponentName, ShareTargetActivityInfo>()
            val appMap = mutableMapOf<String, ShareTargetAppInfo>()

            for (action in actions) {
                for (type in types) {
                    val intent = Intent(action).apply { setType(type) }
                    val resolveInfos = pm.queryIntentActivities(
                        intent,
                        PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS
                    )
                    for (ri in resolveInfos) {
                        val packageName = ri.activityInfo.packageName
                        if (packageName == context.packageName) continue // exclude ourselves

                        val activityName = ri.activityInfo.name
                        val componentName = ComponentName(packageName, activityName)

                        val extActivity = activityMap.getOrPut(componentName) {
                            ShareTargetActivityInfo(
                                componentName = componentName,
                                activityName = ri.loadLabel(pm).toString(),
                                className = ri.activityInfo.name,
                                icon = ri.loadIcon(pm),
                                isEnabled = ri.activityInfo.enabled &&
                                    ri.activityInfo.applicationInfo.enabled,
                                acceptedTypes = mutableSetOf()
                            )
                        }
                        (extActivity.acceptedTypes as MutableSet).add(type)

                        if (!appMap.containsKey(packageName)) {
                            appMap[packageName] = ShareTargetAppInfo(
                                packageName = packageName,
                                appName = ri.activityInfo.applicationInfo.loadLabel(pm).toString(),
                                icon = ri.activityInfo.applicationInfo.loadIcon(pm),
                                isEnabled = ri.activityInfo.applicationInfo.enabled,
                                activities = mutableListOf()
                            )
                        }
                    }
                }
            }

            // assign activities to apps
            activityMap.values.forEach { act ->
                val app = appMap[act.componentName.packageName]
                if (app != null) {
                    (app.activities as MutableList).add(act)
                }
            }

            val currentExcluded =
                prefs.getStringSet(Settings.PREF_EXCLUDED_TARGETS, emptySet()) ?: emptySet()
            appMap.values.toList().sortedWith(compareByDescending<ShareTargetAppInfo> { app ->
                app.activities.any { currentExcluded.contains(it.componentName.flattenToString()) }
            }.thenBy { it.appName })
        }
        appTargets = loadedTargets
        isLoading = false
    }

    LaunchedEffect(excludedTargets) {
        if (!isLoading) {
            prefs.edit { putStringSet(Settings.PREF_EXCLUDED_TARGETS, excludedTargets) }
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.title_manage_share_targets),
        actions = {
            if (!isLoading) {
                val allExpanded = visibleAppTargets.isNotEmpty() &&
                    visibleAppTargets.all { expandedStates[it.packageName] == true }
                val anyExpanded = visibleAppTargets.any { expandedStates[it.packageName] == true }
                Box {
                    IconButton(onClick = { actionsExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.share_target_actions)
                        )
                    }
                    DropdownMenu(
                        expanded = actionsExpanded,
                        onDismissRequest = { actionsExpanded = false },
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.show_disabled_apps)) },
                            trailingIcon = {
                                Checkbox(checked = showDisabledApps, onCheckedChange = null)
                            },
                            onClick = {
                                actionsExpanded = false
                                showDisabledApps = !showDisabledApps
                                prefs.edit {
                                    putBoolean(Settings.PREF_SHOW_DISABLED_APPS, showDisabledApps)
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.expand_all_targets)) },
                            enabled = visibleAppTargets.isNotEmpty() && !allExpanded,
                            onClick = {
                                actionsExpanded = false
                                visibleAppTargets.forEach { expandedStates[it.packageName] = true }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.collapse_all_targets)) },
                            enabled = anyExpanded,
                            onClick = {
                                actionsExpanded = false
                                visibleAppTargets.forEach { expandedStates.remove(it.packageName) }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.clear_inactive_targets, inactiveCount))
                                    Text(
                                        text = stringResource(R.string.clear_inactive_targets_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            enabled = inactiveCount > 0,
                            onClick = {
                                actionsExpanded = false
                                excludedTargets = excludedTargets.filterTo(mutableSetOf()) {
                                    it in activeComponents
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            SettingsList {
                items(visibleAppTargets, key = { it.packageName }) { app ->
                    AppTargetItem(
                        app = app,
                        expanded = expandedStates[app.packageName] == true,
                        onExpandedChange = { expanded ->
                            expandedStates[app.packageName] = expanded
                        },
                        excludedTargets = excludedTargets,
                        onActivitiesStateChanged = { actComponents, isExcluded ->
                            val newSet = excludedTargets.toMutableSet()
                            if (isExcluded) {
                                actComponents.forEach { newSet.add(it.flattenToString()) }
                            } else {
                                actComponents.forEach { newSet.remove(it.flattenToString()) }
                            }
                            excludedTargets = newSet
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppTargetItem(
    app: ShareTargetAppInfo,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    excludedTargets: Set<String>,
    onActivitiesStateChanged: (List<ComponentName>, Boolean) -> Unit
) {
    val excludedCount =
        app.activities.count { excludedTargets.contains(it.componentName.flattenToString()) }
    val totalCount = app.activities.size

    val allExcluded = excludedCount == totalCount
    val exclusionSummary = stringResource(
        when {
            allExcluded -> R.string.share_targets_all_excluded
            excludedCount == 0 -> R.string.share_targets_none_excluded
            else -> R.string.share_targets_partially_excluded
        },
        excludedCount,
        totalCount
    )
    val selectionState = when {
        allExcluded -> ToggleableState.On
        excludedCount == 0 -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Target expansion"
    )
    val hierarchyColor = MaterialTheme.colorScheme.outlineVariant

    PreferenceGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(
                        if (expanded) R.string.collapse_app_targets
                        else R.string.expand_app_targets,
                        app.appName
                    )
                ) { onExpandedChange(!expanded) }
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(chevronRotation)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AndroidView(
                    factory = { ctx -> ImageView(ctx) },
                    update = { it.setImageDrawable(app.icon) },
                    modifier = Modifier.size(40.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!app.isEnabled) {
                        Text(
                            text = stringResource(R.string.share_target_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = exclusionSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectionState == ToggleableState.Indeterminate) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                TriStateCheckbox(
                    state = selectionState,
                    onClick = {
                        onActivitiesStateChanged(
                            app.activities.map { it.componentName },
                            !allExcluded
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = app.appName
                        stateDescription = exclusionSummary
                    }
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val inset = 8.dp.toPx()
                            drawLine(
                                color = hierarchyColor,
                                start = Offset(22.dp.toPx(), inset),
                                end = Offset(22.dp.toPx(), size.height - inset),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        .padding(start = 28.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    app.activities.forEach { act ->
                        val isExcluded = excludedTargets.contains(act.componentName.flattenToString())
                        SwitchPreferenceItem(
                            title = act.activityName,
                            summary = buildString {
                                if (!act.isEnabled) {
                                    append(stringResource(R.string.share_target_disabled))
                                    append('\n')
                                }
                                append(act.className)
                                append('\n')
                                append(act.acceptedTypes.joinToString(", "))
                            },
                            checked = isExcluded,
                            onCheckedChange = { excluded ->
                                onActivitiesStateChanged(listOf(act.componentName), excluded)
                            },
                            leadingContent = {
                                AndroidView(
                                    factory = { ctx -> ImageView(ctx) },
                                    update = { it.setImageDrawable(act.icon) },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
