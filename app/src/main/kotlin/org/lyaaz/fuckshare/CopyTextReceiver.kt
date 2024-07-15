package org.lyaaz.fuckshare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lyaaz.fuckshare.utils.AppUtils


class CopyTextReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
            val text = intent.getStringExtra(EXTRA_TEXT)
            AppUtils.copyToClipboard(context, text!!)
        }
    }

    companion object {
        const val ACTION = "${BuildConfig.APPLICATION_ID}.COPY_TEXT"
        const val EXTRA_TEXT = "EXTRA_TEXT"
    }
}