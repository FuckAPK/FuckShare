package org.baiyu.fuckshare

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import org.baiyu.fuckshare.utils.AppUtils
import org.baiyu.fuckshare.utils.FileUtils
import org.baiyu.fuckshare.utils.IntentUtils
import timber.log.Timber
import java.util.stream.Collectors

class ContentProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        val prefs = AppUtils.getPrefs(this)
        settings = Settings.getInstance(prefs)

        if (settings.toastTime > 0) {
            val applicationName = applicationInfo.loadLabel(packageManager).toString()
            AppUtils.showToast(this, applicationName, settings.toastTime)
        }

        startActivityForResult(setupChooserIntent(), 0)
    }

    override fun finish() {
        AppUtils.scheduleClearCacheWorker(this)
        super.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val resultIntent = Intent()
            data.data?.let { originUri ->
                FileUtils.refreshUri(this, settings, originUri)?.let {
                    resultIntent.data = it
                } ?: {
                    Timber.e("Failed to process $originUri")
                    AppUtils.showToast(
                        this,
                        resources.getString(R.string.fail_to_process).format(1, 1),
                        settings.toastTime
                    )
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
            data.clipData?.let { clipData ->
                val uris = (0 until clipData.itemCount)
                    .mapNotNull { clipData.getItemAt(it).uri }

                val resultUris = uris
                    .parallelStream()
                    .map { FileUtils.refreshUri(this, settings, it) }
                    .filter { it != null }
                    .map { ClipData.Item(it) }
                    .collect(Collectors.toList())

                (uris.count() - resultUris.size).let {
                    if (it > 0) {
                        Timber.e("Failed to process $it of ${uris.count()}")
                        AppUtils.showToast(
                            this,
                            resources.getString(R.string.fail_to_process).format(it, uris.count()),
                            settings.toastTime
                        )
                    }
                }

                if (resultUris.isNotEmpty()) {
                    val resultClipData = ClipData(clipData.description, resultUris.removeAt(0))
                    resultUris.forEach { resultClipData.addItem(it) }
                    resultIntent.clipData = resultClipData
                } else {
                    Timber.w("result uris is empty")
                    setResult(RESULT_CANCELED)
                    finish()
                }

            }
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
            Timber.d("result intent: $resultIntent")
        } else {
            setResult(RESULT_CANCELED)
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

        IntentUtils.getParcelableArrayExtra<Intent>(
            intent,
            Intent.EXTRA_INITIAL_INTENTS
        )?.map {
            cloneIntent(it)
        }?.toTypedArray()?.let {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, it)
            Timber.d("${Intent.EXTRA_INITIAL_INTENTS}: $it")
        }

        return chooserIntent
    }

    private fun cloneIntent(intent: Intent): Intent {
        return Intent().apply {
            action = intent.action
            identifier = intent.identifier
            setDataAndType(intent.data, intent.type)
            intent.categories?.forEach { addCategory(it) }

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