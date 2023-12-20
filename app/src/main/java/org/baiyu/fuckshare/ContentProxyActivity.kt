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

    private fun setupChooserIntent(): Intent {
        Timber.d("origin intent: %s", intent.toString())
        val pickIntent = cloneIntent(intent)

        Timber.d("new intent: %s", pickIntent.toString())
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
            Timber.d("%s: %s", Intent.EXTRA_INITIAL_INTENTS, it.toString())
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

    override fun finish() {
        AppUtils.scheduleClearCacheWorker(this)
        super.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val resultIntent = Intent()
            data.data?.let {
                resultIntent.data = FileUtils.refreshUri(this, settings, it)
            }
            data.clipData?.let { clipData ->
                val resultUris = (0 until clipData.itemCount)
                    .asSequence()
                    .map { clipData.getItemAt(it).uri }
                    .map { FileUtils.refreshUri(this, settings, it) }
                    .filterNotNull()
                    .map { ClipData.Item(it) }
                    .toMutableList()

                val resultClipData = ClipData(clipData.description, resultUris.removeAt(0))
                resultUris.forEach { resultClipData.addItem(it) }
                resultIntent.clipData = resultClipData
            }
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
            Timber.d("intent: %s", resultIntent.toString())
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        private lateinit var settings: Settings
    }

}