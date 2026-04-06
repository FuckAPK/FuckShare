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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lyaaz.fuckshare.utils.AppUtils
import org.lyaaz.ui.theme.AppTheme

data class ShareTargetActivityInfo(
    val componentName: ComponentName,
    val activityName: String,
    val className: String,
    val icon: android.graphics.drawable.Drawable,
    val acceptedTypes: Set<String>
)

data class ShareTargetAppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTargetsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppUtils.getPrefs(context) }
    var excludedTargets by remember {
        mutableStateOf(prefs.getStringSet(Settings.PREF_EXCLUDED_TARGETS, emptySet()) ?: emptySet())
    }

    var appTargets by remember { mutableStateOf<List<ShareTargetAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val types =
                listOf("*/*", "text/plain", "image/*", "video/*", "audio/*", "application/*")
            val actions = listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)

            val activityMap = mutableMapOf<ComponentName, ShareTargetActivityInfo>()
            val appMap = mutableMapOf<String, ShareTargetAppInfo>()

            for (action in actions) {
                for (type in types) {
                    val intent = Intent(action).apply { setType(type) }
                    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
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
                                acceptedTypes = mutableSetOf()
                            )
                        }
                        (extActivity.acceptedTypes as MutableSet).add(type)

                        if (!appMap.containsKey(packageName)) {
                            appMap[packageName] = ShareTargetAppInfo(
                                packageName = packageName,
                                appName = ri.activityInfo.applicationInfo.loadLabel(pm).toString(),
                                icon = ri.activityInfo.applicationInfo.loadIcon(pm),
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
            appTargets =
                appMap.values.toList().sortedWith(compareByDescending<ShareTargetAppInfo> { app ->
                    app.activities.any { currentExcluded.contains(it.componentName.flattenToString()) }
                }.thenBy { it.appName })

            isLoading = false
        }
    }

    LaunchedEffect(excludedTargets) {
        if (!isLoading) {
            prefs.edit { putStringSet(Settings.PREF_EXCLUDED_TARGETS, excludedTargets) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.title_manage_share_targets)) },
                actions = {
                    if (!isLoading) {
                        val allExpanded =
                            appTargets.isNotEmpty() && expandedStates.size == appTargets.size && expandedStates.values.all { it }
                        IconButton(onClick = {
                            if (allExpanded) {
                                expandedStates.clear()
                            } else {
                                appTargets.forEach { expandedStates[it.packageName] = true }
                            }
                        }) {
                            Icon(
                                imageVector = if (allExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = if (allExpanded) "Collapse All" else "Expand All"
                            )
                        }
                        IconButton(onClick = {
                            val activeComponents = appTargets.flatMap { it.activities }
                                .map { it.componentName.flattenToString() }.toSet()
                            val validExcluded =
                                excludedTargets.filter { activeComponents.contains(it) }.toSet()
                            if (validExcluded.size != excludedTargets.size) {
                                excludedTargets = validExcluded
                            }
                        }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                contentDescription = "Clear Inactive"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(appTargets, key = { it.packageName }) { app ->
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
                    item {
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
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

    val checkState = when {
        excludedCount == totalCount -> androidx.compose.ui.state.ToggleableState.On
        excludedCount == 0 -> androidx.compose.ui.state.ToggleableState.Off
        else -> androidx.compose.ui.state.ToggleableState.Indeterminate
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply { setImageDrawable(app.icon) }
                },
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            TriStateCheckbox(
                state = checkState,
                onClick = {
                    val newState = checkState != androidx.compose.ui.state.ToggleableState.On
                    onActivitiesStateChanged(app.activities.map { it.componentName }, newState)
                }
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 16.dp, bottom = 8.dp)
            ) {
                app.activities.forEach { act ->
                    val isExcluded = excludedTargets.contains(act.componentName.flattenToString())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply { setImageDrawable(act.icon) }
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = act.activityName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = act.className,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                act.acceptedTypes.forEach { type ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = type,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = isExcluded,
                            onCheckedChange = { checked ->
                                onActivitiesStateChanged(listOf(act.componentName), checked)
                            }
                        )
                    }
                }
            }
        }
    }
}
