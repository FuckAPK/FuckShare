package org.lyaaz.fuckshare

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.lyaaz.fuckshare.utils.AppUtils
import org.lyaaz.fuckshare.utils.FileUtils
import org.lyaaz.fuckshare.utils.IntentUtils
import timber.log.Timber
import java.util.stream.Collectors

class ContentProxyActivity : ComponentActivity() {

    val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            pickerResult(it.resultCode, it.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppUtils.timberPlantTree(this)

        val prefs = AppUtils.getPrefs(this)
        settings = Settings.getInstance(prefs)

        if (settings.toastTimeMS > 0) {
            val applicationName = applicationInfo.loadLabel(packageManager).toString()
            AppUtils.showToast(this, applicationName, settings.toastTimeMS)
        }

        resultLauncher.launch(setupChooserIntent())
    }

    override fun finish() {
        AppUtils.scheduleClearCacheWorker(this)
        super.finish()
    }

    fun pickerResult(resultCode: Int, data: Intent?) {
        Timber.d("data: $data")
        if (resultCode != RESULT_OK || data == null) {
            Timber.i("Result Code: $resultCode, data: $data")
            setResult(resultCode)
            finish()
            return
        }

        val resultIntent = Intent()
        data.data?.let { originUri ->
            FileUtils.refreshUri(this, settings, originUri)?.let {
                resultIntent.data = it.first
            } ?: run {
                Timber.e("Failed to process $originUri")
            }
        } ?: run {
            Timber.i("data empty")
        }
        data.clipData?.let { clipData ->
            val uris = (0 until clipData.itemCount)
                .mapNotNull { clipData.getItemAt(it).uri }

            val resultUris = uris
                .parallelStream()
                .map { FileUtils.refreshUri(this, settings, it) }
                .collect(Collectors.toList())
                .also {
                    val failedCount = it.count { p -> p == null }
                    if (failedCount > 0) {
                        Timber.e("Failed to process $it of ${uris.count()}")
                        AppUtils.showToast(
                            this,
                            resources.getString(R.string.fail_to_process).format(it, uris.count()),
                            settings.toastTimeMS
                        )
                    }
                }
                .filterNotNull()
                .map { ClipData.Item(it.first) }

            if (resultUris.isNotEmpty()) {
                val resultClipData = ClipData(clipData.description, resultUris[0])
                resultUris.drop(1).forEach { resultClipData.addItem(it) }
                resultIntent.clipData = resultClipData
            } else {
                Timber.w("result uris is empty")
            }
        } ?: run {
            Timber.i("clipdata empty")
        }

        if (resultIntent.data == null && resultIntent.clipData == null) {
            setResult(RESULT_CANCELED)
            Timber.w("result empty, cancelled")
        } else {
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
            Timber.d("result intent: $resultIntent")
        }
        finish()
    }

    private fun setupChooserIntent(): Intent {
        Timber.d("origin intent: $intent")
        val pickIntent = cloneIntent(intent)

        Timber.d("new intent: $pickIntent")
        val chooserIntent = Intent.createChooser(pickIntent, resources.getString(R.string.app_name))
            .putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                listOf(ComponentName(this, this::class.java)).toTypedArray()
            )

        setOf(Intent.EXTRA_INITIAL_INTENTS, Intent.EXTRA_ALTERNATE_INTENTS).forEach {
            IntentUtils.restoreArrayExtras<Intent>(
                intent,
                chooserIntent,
                it
            )
        }

        return chooserIntent
    }

    private fun cloneIntent(intent: Intent): Intent {
        return Intent().apply {

            action = intent.action
            type = intent.type
            intent.categories?.forEach { addCategory(it) }
            clipData = intent.clipData

            if (intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            if (intent.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY, false)) {
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.let {
                putExtra(Intent.EXTRA_MIME_TYPES, it)
            }
        }
    }

    companion object {
        private lateinit var settings: Settings
    }
}