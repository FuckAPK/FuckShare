package org.baiyu.fuckshare

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import timber.log.Timber

class ContentProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        val prefs = Utils.getPrefs(this)
        settings = Settings.getInstance(prefs)

        val pickIntent = Intent().apply {
            action = intent.action
            type = intent.type
            intent.categories?.forEach { addCategory(it) }
            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            )
        }
        Timber.d(intent.toString())
        startActivityForResult(
            Intent.createChooser(pickIntent, "").putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                listOf(ComponentName(this, this::class.java)).toTypedArray()
            ), 0
        )
    }

    override fun finish() {
        Utils.setWorker(this)
        super.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val resultIntent = Intent()
            data.data?.let {
                resultIntent.data = Utils.refreshUri(this, settings, it)
            }
            data.clipData?.let { clipData ->
                val resultUris = (0 until clipData.itemCount)
                    .asSequence()
                    .map { clipData.getItemAt(it).uri }
                    .map { Utils.refreshUri(this, settings, it) }
                    .filterNotNull()
                    .map { ClipData.Item(it) }
                    .toMutableList()

                val resultClipData = ClipData(clipData.description, resultUris.removeAt(0))
                resultUris.forEach { resultClipData.addItem(it) }
                resultIntent.clipData = resultClipData
            }
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        private lateinit var settings: Settings
    }

}