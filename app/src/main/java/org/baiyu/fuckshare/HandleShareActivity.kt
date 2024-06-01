package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ShareCompat.IntentBuilder
import org.baiyu.fuckshare.utils.AppUtils
import org.baiyu.fuckshare.utils.FileUtils
import org.baiyu.fuckshare.utils.IntentUtils
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.atomic.AtomicInteger

class HandleShareActivity : Activity() {
    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            Timber.plant(DebugTree())
        }
        val prefs = AppUtils.getPrefs(this)
        settings = Settings.getInstance(prefs)

        if (settings.toastTime > 0) {
            val applicationName = applicationInfo.loadLabel(packageManager).toString()
            AppUtils.showToast(this, applicationName, settings.toastTime)
        }

        if ("text/plain" == intent.type) {
            handleText(intent)
        } else {
            IntentUtils.getUrisFromIntent(intent)?.let {
                handleUris(it)
            } ?: Timber.d("Uri is empty: $intent")
        }
        finish()
    }

    override fun finish() {
        AppUtils.scheduleClearCacheWorker(this)
        super.finish()
    }

    private fun handleText(intent: Intent) {
        val ib = IntentBuilder(this)
        ib.setType(intent.type)
        ib.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT))
        val chooserIntent = ib.setChooserTitle(R.string.app_name).createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, this::class.java)).toTypedArray()
        )
        startActivity(chooserIntent)
    }

    private fun handleUris(uris: List<Uri?>) {
        val ib = IntentBuilder(this).apply {
            intent.apply {
                this@HandleShareActivity.intent.let {
                    type = it.type
                    clipData = it.clipData
                }
            }
        }

        val nullCount = AtomicInteger(0)
        uris.filterNotNull()
            .parallelStream()
            .map { FileUtils.refreshUri(this, settings, it) }
            .forEachOrdered {
                it?.let { ib.addStream(it) }
                    ?: nullCount.incrementAndGet()
            }

        nullCount.get().let {
            if (it > 0) {
                Timber.e("Failed to process $it of ${uris.size}")
                AppUtils.showToast(
                    this,
                    resources.getString(R.string.fail_to_process).format(it, uris.size),
                    settings.toastTime
                )
            }
        }

        val chooserIntent = ib.setChooserTitle(R.string.app_name).createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, this::class.java)).toTypedArray()
        )
        Timber.d("chooser intent: $chooserIntent")
        startActivity(chooserIntent)
    }

    companion object {
        private lateinit var settings: Settings
    }
}