package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ShareCompat.IntentBuilder
import timber.log.Timber
import timber.log.Timber.DebugTree

class HandleShareActivity : Activity() {
    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            Timber.plant(DebugTree())
        }
        val prefs = Utils.getPrefs(this)
        settings = Settings.getInstance(prefs)

        if ("text/plain" == intent.type) {
            handleText(intent)
        } else {
            val uris = Utils.getUrisFromIntent(intent)!!
            handleUris(uris)
        }
        finish()
    }

    override fun finish() {
        Utils.setWorker(this)
        super.finish()
    }

    private fun handleText(intent: Intent) {
        val ib = IntentBuilder(this)
        ib.setType(intent.type)
        ib.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT))
        val chooserIntent = ib.createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, this::class.java)).toTypedArray()
        )
        startActivity(chooserIntent)
    }

    private fun handleUris(uris: List<Uri?>) {
        val ib = IntentBuilder(this).setType(intent.type)
        uris.filterNotNull()
            .parallelStream()
            .map { Utils.refreshUri(this, settings, it) }
            .forEachOrdered { it?.let { ib.addStream(it) } }

        val chooserIntent = ib.createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, this::class.java)).toTypedArray()
        )
        Timber.d("intent: %s", chooserIntent.toString())
        startActivity(chooserIntent)
    }

    companion object {
        private lateinit var settings: Settings
    }
}