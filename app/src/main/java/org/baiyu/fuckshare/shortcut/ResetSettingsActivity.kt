package org.baiyu.fuckshare.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import org.baiyu.fuckshare.R
import org.baiyu.fuckshare.utils.AppUtils

class ResetSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppUtils.getPrefs(this)
        prefs.edit { clear() }
        Toast.makeText(this, R.string.toast_settings_reset, Toast.LENGTH_SHORT).show()
        finish()
    }
}