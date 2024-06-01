package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserAction
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

        handleIntent()
        finish()
    }

    override fun finish() {
        AppUtils.scheduleClearCacheWorker(this)
        super.finish()
    }

    private fun handleIntent() {
        val uris = IntentUtils.getUrisFromIntent(intent)
        val ib = IntentBuilder(this).apply {
            setType(this@HandleShareActivity.intent.type)
            this@HandleShareActivity.intent.getStringExtra(Intent.EXTRA_TEXT)?.let { setText(it) }
        }

        val nullCount = AtomicInteger(0)
        uris.parallelStream()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            IntentUtils.getParcelableArrayExtra<ChooserAction>(
                intent,
                Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
            )?.let {
                chooserIntent.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, it)
            }
        }
        Timber.d("chooser intent: $chooserIntent")
        startActivity(chooserIntent)
    }

    companion object {
        private lateinit var settings: Settings
    }
}